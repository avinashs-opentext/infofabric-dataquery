/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.guice;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opentext.infofabric.appstate.client.AppStateClient;
import com.opentext.infofabric.appstate.client.AppStateClientBuilder;
import com.opentext.infofabric.common.crypto.IFabricCryptoService;
import com.opentext.infofabric.datamodel.ModelClient;
import com.opentext.infofabric.dataquery.DataqueryConfiguration;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.NamedQueryConfiguration;
import com.opentext.infofabric.dataquery.cache.ResponseCache;
import com.opentext.infofabric.dataquery.cache.ResponseCacheFilter;
import com.opentext.infofabric.dataquery.endpoints.DataQueryServlet;
import com.opentext.infofabric.dataquery.graphql.GraphQLService;
import com.opentext.infofabric.dataquery.graphql.RootDataAccess;
import com.opentext.infofabric.dataquery.graphql.RootDataMutator;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.HBaseConnection;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.HBaseConnectionImpl;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.HBaseQuery;
import com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms.RDBMSConnectionProvider;
import com.opentext.infofabric.dataquery.graphql.mutation.DatacastTransactionServiceImpl;
import com.opentext.infofabric.dataquery.graphql.mutation.TransactionService;
import com.opentext.infofabric.dataquery.mock.MockModelClient;
import com.opentext.infofabric.dataquery.services.ModelService;
import com.opentext.infofabric.dataquery.services.NamedQueryConnectionProvider;
import com.opentext.infofabric.dataquery.services.NamedQueryExecutorService;
import com.opentext.infofabric.dataquery.services.NamedQueryService;
import com.opentext.infofabric.dataquery.services.impl.ModelServiceImpl;
import com.opentext.infofabric.dataquery.services.impl.NamedQueryConnectionProviderImpl;
import com.opentext.infofabric.dataquery.services.impl.NamedQueryExecutorServiceImpl;
import com.opentext.infofabric.dataquery.services.impl.NamedQueryServiceImpl;
import com.opentext.infofabric.dataquery.util.ResultSetStreamUtil;
import com.opentext.infofabric.registrar.client.RegistrarClient;
import com.opentext.infofabric.registrar.stream.IFabricKafkaAdminClient;
import com.opentext.infofabric.registrar.stream.StreamProducer;
import com.opentext.infofabric.registrar.types.ApplicationInfo;
import com.opentext.infofabric.registrar.types.HostAndPort;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Environment;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

//import com.liaison.datagate.fcl.security.DataGateSecurity;
//import com.liaison.datagate.fcl.security.DataGateSecurityBuilder;
//import com.liaison.datagate.rdbms.fcl.RDBMSControl;

public class DataqueryModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(DataqueryModule.class);

    private final DataqueryConfiguration config;
    private final IFabricCryptoService cryptoService;
    private ApplicationInfo appInfoDataquery;

    private final Environment environment;

    public DataqueryModule(DataqueryConfiguration config, Environment environment) {
        this.config = config;
        this.cryptoService = provideIFabricCryptoService();

        this.environment = environment;
        appInfoDataquery = new ApplicationInfo(
                DataqueryConstants.APP_NAME,
                config.getEnvironment().getDataCenter(),
                config.getEnvironment().getEnvironment(),
                new HostAndPort(config.getHostName(), getApplicationPort()));
    }

    @Override
    protected void configure() {
        bind(ModelService.class).to(ModelServiceImpl.class);
        bind(NamedQueryService.class).to(NamedQueryServiceImpl.class);
        bind(HBaseConnection.class).to(HBaseConnectionImpl.class);
        bind(TransactionService.class).to(DatacastTransactionServiceImpl.class);
        bind(NamedQueryConnectionProvider.class).to(NamedQueryConnectionProviderImpl.class);
        requestStaticInjection(RDBMSConnectionProvider.class);
        requestStaticInjection(HBaseQuery.class);
        requestStaticInjection(RootDataMutator.class);
        requestStaticInjection(DataQueryServlet.class);
        requestStaticInjection(ResponseCacheFilter.class);
    }

    @Provides
    @Singleton
    DataqueryConfiguration provideConfig() {
        return config;
    }

    @Provides
    @Singleton
    RootDataAccess.QueryModel provideQueryModel() {
        return config.getDefaultQueryModel();
    }

    @Provides
    @Singleton
    ResponseCache provideCache() {
        return new ResponseCache(config);
    }

    @Provides
    @Singleton
    ModelClient provideModelClient() {
        Map<String, Object> datamodelConfig = config.getDatamodelConfig();

        if (MapUtils.isEmpty(datamodelConfig) || Boolean.TRUE.equals(datamodelConfig.get("mockCompiler"))) {
            LOG.warn("USING MOCK DATA MODEL CLIENT! NOT FOR PRODUCTION!!!");
            return new MockModelClient();
        }
        return ModelClient.instance(datamodelConfig);
    }

    @Provides
    @Singleton
    NamedQueryExecutorService provideNamedQueryExecutorService() {
        return new NamedQueryExecutorServiceImpl();
    }
    
    @Provides
    @Singleton
    IFabricKafkaAdminClient provideAdminClient() {
        return new IFabricKafkaAdminClient(config.getAdmin());
    }

    @Provides
    @Singleton
    AppStateClient provideAppStateClient() throws UnknownHostException {
        NamedQueryConfiguration config = NamedQueryConfiguration.getInstance();
        Map<String, Map<String, Object>> appStateConfig = config.getAppState();
        Map<String, Object> appStateStreamConfig = appStateConfig.get("stream");
        Map<String, Object> appStateStorageConfig = appStateConfig.get("storage");
        String hostName = InetAddress.getLocalHost().getHostName();
        String applicationId = (String) appStateConfig.get("application").getOrDefault("name", "dataquery");
        String groupIdSuffix = (String) appStateStreamConfig.getOrDefault("consumer_groupid_suffix", applicationId);
        AppStateClientBuilder builder = AppStateClient.builder(config.getEnvironment(), applicationId, hostName + groupIdSuffix);
        LOG.debug("Providing AppStateClient for applicationId {} and instanceId {}{}", applicationId, hostName, groupIdSuffix);
        builder = builder.streamAdminConfig(appStateStreamConfig).defaultStorage();
        builder = builder.kafkaStream()
                .kafkaServers((String) appStateStreamConfig.get(DataqueryConstants.BOOTSTRAP_SERVER));

        if (StringUtils.equalsIgnoreCase(
                (CharSequence) appStateStorageConfig.getOrDefault("type", "ARANGODB"),
                "ARANGODB")) {
            builder = builder.storageHosts((String) appStateStorageConfig.get("hosts"))
                    .storageSSL((Boolean) appStateStorageConfig.getOrDefault("useSSL", true))
                    .storageUser((String) appStateStorageConfig.get("user"))
                    .storagePassword((String) appStateStorageConfig.get(DataqueryConstants.DATASTREAM_PASSWORD));
        } else {
            builder = builder.inMemory();
        }
        return builder.buildDefaultClient();
    }

	@Provides
	@Singleton
	CloseableHttpClient provideHttpClientDataCast() {
		return new HttpClientBuilder(environment).using(config.getHttpClientConfiguration()).build("httpClient");
	}

	@Provides
	@Singleton
	GraphQLService provideGraphQLService() {
		return new GraphQLService();
	}


    @Provides
    @Singleton
    IFabricCryptoService provideIFabricCryptoService() {
        return IFabricCryptoService.getInstance();
    }


    @Provides
    @Singleton
    StreamProducer provideStreamProducer() {

        return new StreamProducer.Builder()
                .setApplicationInfo(appInfoDataquery)
                //.setGatewayResponseTopic("")
                .setStreamConfig(
                        ImmutableMap.<String, Object>builder()
                                .put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getAdmin().get(DataqueryConstants.BOOTSTRAPSERVER))
                                .build())
                .build();
    }

    @Provides
    @Singleton
    RegistrarClient provideRegistrarClient() {

        RegistrarClient.Builder builder = new RegistrarClient.Builder();
        builder.setApplicationInfo(appInfoDataquery);
        builder.setStreamConfig(
                ImmutableMap.<String, Object>builder()
                        .put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getRegistrarClient().get(DataqueryConstants.BOOTSTRAPSERVER))
                        .build());//.setRegistrarTopic("registrar.topic")
        return builder
                .build();
    }

    @Provides
    @Singleton
    ResultSetStreamUtil provideResultSetStreamUtil() {
        return new ResultSetStreamUtil();
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
