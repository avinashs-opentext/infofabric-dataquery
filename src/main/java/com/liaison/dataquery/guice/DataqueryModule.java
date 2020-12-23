/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.guice;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import com.opentext.infofabric.appstate.client.AppStateClient;
import com.opentext.infofabric.appstate.client.AppStateClientBuilder;
import com.opentext.infofabric.common.crypto.IFabricCryptoService;
import com.opentext.infofabric.datamodel.ModelClient;
import com.opentext.infofabric.registrar.stream.IFabricKafkaAdminClient;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.liaison.datagate.fcl.security.DataGateSecurity;
import com.liaison.datagate.fcl.security.DataGateSecurityBuilder;
import com.liaison.datagate.rdbms.fcl.RDBMSControl;
import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.NamedQueryConfiguration;
import com.liaison.dataquery.cache.ResponseCache;
import com.liaison.dataquery.cache.ResponseCacheFilter;
import com.liaison.dataquery.endpoints.DataQueryServlet;
import com.liaison.dataquery.graphql.GraphQLService;
import com.liaison.dataquery.graphql.RootDataAccess;
import com.liaison.dataquery.graphql.RootDataMutator;
import com.liaison.dataquery.graphql.dataloaders.hbase.HBaseConnection;
import com.liaison.dataquery.graphql.dataloaders.hbase.HBaseConnectionImpl;
import com.liaison.dataquery.graphql.dataloaders.hbase.HBaseQuery;
import com.liaison.dataquery.graphql.dataloaders.rdbms.RDBMSConnectionProvider;
import com.liaison.dataquery.graphql.mutation.DatacastTransactionServiceImpl;
import com.liaison.dataquery.graphql.mutation.TransactionService;
import com.liaison.dataquery.mock.MockModelClient;
import com.liaison.dataquery.services.ModelService;
import com.liaison.dataquery.services.NamedQueryConnectionProvider;
import com.liaison.dataquery.services.NamedQueryExecutorService;
import com.liaison.dataquery.services.NamedQueryService;
import com.liaison.dataquery.services.impl.ModelServiceImpl;
import com.liaison.dataquery.services.impl.NamedQueryConnectionProviderImpl;
import com.liaison.dataquery.services.impl.NamedQueryExecutorServiceImpl;
import com.liaison.dataquery.services.impl.NamedQueryServiceImpl;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Environment;

public class DataqueryModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(DataqueryModule.class);

    private final DataqueryConfiguration config;
    private Map<String, Object> ssoClientConfig;
    private IFabricCryptoService cryptoService;

    private final Environment environment;

    public DataqueryModule(DataqueryConfiguration config, Environment environment) {
        this.config = config;
        this.cryptoService = provideIFabricCryptoService();

        this.ssoClientConfig = config.getDecryptedASsoClientConfig();
        this.environment = environment;

//        if (!config.isEnableSSO() || ssoClientConfig == null || Boolean.TRUE.equals(ssoClientConfig.get("mock"))) {
//            LOG.warn("USING MOCK SSOClient FOR RDBMSControl! NOT FOR PRODUCTION!!!");
//            RDBMSControl.setSsoClient(AlloySSOClientBuilder.mock());
//        } else {
//            RDBMSControl.setSsoClient(ssoClientConfig.get("identityGatewayUrl").toString(),
//                    (Integer) ssoClientConfig.get("cacheSize"),
//                    (Integer) ssoClientConfig.get("cacheTtl"),
//                    ssoClientConfig.get("clientId").toString(),
//                    (String) ssoClientConfig.get("clientSecret"));
//        }
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
    DataGateSecurity provideDataGateSecurity() {
        if (!config.isEnableSSO() || ssoClientConfig == null || Boolean.TRUE.equals(ssoClientConfig.get("mock"))) {
            LOG.warn("USING MOCK DATA GATE SECURITY CLIENT! NOT FOR PRODUCTION!!!");
            return DataGateSecurityBuilder.mock();
        }

        return DataGateSecurityBuilder.create(ssoClientConfig.get("identityGatewayUrl").toString(),
                (Integer) ssoClientConfig.get("cacheSize"),
                (Integer) ssoClientConfig.get("cacheTtl"),
                ssoClientConfig.get("clientId").toString(),
                (String) ssoClientConfig.get("clientSecret")).build();
    }

    @Provides
    @Singleton
    NamedQueryExecutorService provideNamedQueryExecutorService() {
        return new NamedQueryExecutorServiceImpl();
    }
    
    @Provides
    @Singleton
    IFabricKafkaAdminClient provideAdminClient() {
//        NamedQueryConfiguration config = NamedQueryConfiguration.getInstance();
//        if (config.getStreamImplementation().equals(ConfluxFactoryWrapper.StreamType.MAPR.name())){
//            return ConfluxFactory.getMaprAdminClient(config.getMaprAdminClientConfig());
//        }
//        else{
//            return ConfluxFactory.getKafkaAdminClient(config.getKafkaAdminClient());
//        }

        return new IFabricKafkaAdminClient(config.getKafkaAdminClient());
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
//        LOG.debug("Providing AppStateClient for applicationId {} and instanceId {}{}", applicationId, hostName, groupIdSuffix);
        builder = builder.streamAdminConfig(appStateStreamConfig).defaultStorage();
//        if ((StringUtils.equalsIgnoreCase(
//                (CharSequence) appStateStreamConfig.getOrDefault("type", "STREAM"),
//                "STREAM")) && (config.getStreamImplementation().equals(ConfluxFactoryWrapper.StreamType.MAPR.name())) ) {
//            builder = builder.maprServers((String) appStateStreamConfig.get(DataqueryConstants.DATASTREAM_SERVER))
//                    .maprUser((String) appStateStreamConfig.get("user"))
//                    .maprPassword((String) appStateStreamConfig.get(DataqueryConstants.DATASTREAM_PASSWORD));
//        }
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

}
