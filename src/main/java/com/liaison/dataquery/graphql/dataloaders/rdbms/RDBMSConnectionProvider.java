/**
 * Copyright 2017 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.dataloaders.rdbms;

import com.google.inject.Inject;
import com.liaison.dataquery.DataqueryConfiguration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.prometheus.client.Histogram;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static com.liaison.dataquery.DataqueryConstants.PROMETHEUS_METRICS_ROOT;

public class RDBMSConnectionProvider {
    private static final Logger logger = LoggerFactory.getLogger(RDBMSConnectionProvider.class);
    private static final Map<String, HikariDataSource> connectionPools;

    static final Histogram connectionLatency = Histogram.build()
            .name(PROMETHEUS_METRICS_ROOT + "rdbms_connection_provider_latency_seconds")
            .help("RDBMSConnectionProvider latency in seconds.")
            .register();

    @Inject
    private static DataqueryConfiguration configuration;

    private final String md5hash;

    static {
        connectionPools = new ConcurrentHashMap<>();
    }

    private static String defaultUser;
    private static String defaultPassword;

    public static RDBMSConnectionProvider forTenant(String tenant) {
        String url = (String) configuration.getRdbms().get("url");
        if (tenant != null && !tenant.isEmpty()) {
            url = String.format("%sdc_%s", url, tenant);
            String dbSuffix = (String) configuration.getRdbms().get("databaseSuffix");
            if (dbSuffix != null && !dbSuffix.isEmpty()) {
                url = String.format("%s%s", url, dbSuffix);
            }
        }
        if (defaultUser == null) {
            defaultUser = (String) configuration.getRdbms().get("user");
        }
        if (defaultPassword == null) {
            defaultPassword = (String) configuration.getDecryptedRdbmsConfig().get("password");
        }
        return new RDBMSConnectionProvider(url, defaultUser, defaultPassword);
    }

    public RDBMSConnectionProvider(String url, String username, String password) {
        md5hash = DigestUtils.md5Hex(String.format("%s@@%s@@%s", username, url, password));
        //setting below property would do case insensitive comparision for citext fields.
        String newUrl = url + "?stringtype=unspecified";

        connectionPools.computeIfAbsent(md5hash, key -> {
            logger.debug(String.format("Hash %s didn't match any of the %s already created pools, creating new pool to %s", md5hash, connectionPools.size(), newUrl));
            Properties hikariProperties = new Properties();
            hikariProperties.putAll((Map<?, ?>) configuration.getRdbms().get("hikari"));
            Properties hikariDataSourceProperties = new Properties();
            hikariDataSourceProperties.putAll((Map<?, ?>) configuration.getRdbms().get("hikariDataSource"));
            HikariConfig hikariConfig = new HikariConfig(hikariProperties);
            hikariConfig.setAutoCommit(false);
            hikariConfig.addDataSourceProperty("url", newUrl);
            hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
            hikariConfig.addDataSourceProperty("user", username);
            hikariConfig.addDataSourceProperty("password", password);
            hikariConfig.addDataSourceProperty("ssl", hikariDataSourceProperties.get("ssl") != null ? hikariDataSourceProperties.get("ssl") : "true");
            if (hikariDataSourceProperties.get("insecure") != null && hikariDataSourceProperties.get("insecure").equals("true")) {
                hikariConfig.addDataSourceProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
            }
            return new HikariDataSource(hikariConfig);
        });
    }

    public Connection getConnection() throws SQLException {
        Histogram.Timer timer = connectionLatency.startTimer();
        Connection connection = connectionPools.get(md5hash).getConnection();
        timer.observeDuration();
        return connection;
    }

}
