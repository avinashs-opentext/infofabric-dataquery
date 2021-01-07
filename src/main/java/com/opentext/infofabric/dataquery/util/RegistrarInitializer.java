/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.util;

import com.google.inject.Inject;
import com.opentext.infofabric.dataquery.DataqueryApplication;
import com.opentext.infofabric.dataquery.DataqueryConfiguration;
import com.opentext.infofabric.registrar.types.ApplicationInfo;
import com.opentext.infofabric.registrar.types.HostAndPort;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

//import com.opentext.infofabric.registrar.Registrar;

public class RegistrarInitializer {
    private static final Logger log = LoggerFactory.getLogger(RegistrarInitializer.class);
    private final DataqueryConfiguration config;

    @Inject
    public RegistrarInitializer(DataqueryConfiguration config) {
        this.config = config;
    }

    public void initialize() {
        final ApplicationInfo appInfo;
        try {
            appInfo = new ApplicationInfo(
                    DataqueryApplication.APP_NAME,
                    config.getEnvironment().getDataCenter(),
                    config.getEnvironment().getEnvironment(),
                    new HostAndPort(InetAddress.getLocalHost().getHostName(), getApplicationPort()));
//            Registrar.initialize(appInfo);
            log.info("Registrar initialized.");
        } catch (UnknownHostException e) {
            log.error("Registrar Initialization error", e);
        }

    }

    private Integer getApplicationPort() {
        DefaultServerFactory serverFactory = (DefaultServerFactory) config.getServerFactory();
        for (ConnectorFactory connector : serverFactory.getAdminConnectors()) {
            if (connector.getClass().isAssignableFrom(HttpConnectorFactory.class)) {
                return ((HttpConnectorFactory) connector).getPort();
            }
        }
        return 9443;
    }
}
