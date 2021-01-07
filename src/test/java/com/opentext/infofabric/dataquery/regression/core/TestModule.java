/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.regression.core;

import com.opentext.infofabric.dataquery.DataqueryConfiguration;
import com.opentext.infofabric.dataquery.cache.ResponseCacheFilter;
import com.opentext.infofabric.dataquery.endpoints.DataQueryServlet;
import com.opentext.infofabric.dataquery.graphql.RootDataMutator;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.HBaseConnection;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.HBaseQuery;
import com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms.RDBMSConnectionProvider;
import com.opentext.infofabric.dataquery.graphql.mutation.TransactionService;
import com.opentext.infofabric.dataquery.guice.DataqueryModule;
import com.opentext.infofabric.dataquery.regression.mock.MockHBaseConnectionImpl;
import com.opentext.infofabric.dataquery.regression.mock.MockModelServiceImpl;
import com.opentext.infofabric.dataquery.regression.mock.MockTransactionServiceImpl;
import com.opentext.infofabric.dataquery.services.ModelService;
import com.opentext.infofabric.dataquery.services.NamedQueryConnectionProvider;
import com.opentext.infofabric.dataquery.services.NamedQueryService;
import com.opentext.infofabric.dataquery.services.impl.NamedQueryConnectionProviderImpl;
import com.opentext.infofabric.dataquery.services.impl.NamedQueryServiceImpl;
import io.dropwizard.setup.Environment;

public class TestModule extends DataqueryModule {
    public TestModule(DataqueryConfiguration config, Environment environment) {
        super(config, environment);
    }

    @Override
    protected void configure() {
        bind(ModelService.class).to(MockModelServiceImpl.class);
        bind(NamedQueryService.class).to(NamedQueryServiceImpl.class);
        bind(HBaseConnection.class).to(MockHBaseConnectionImpl.class);
        bind(TransactionService.class).to(MockTransactionServiceImpl.class);
        bind(NamedQueryConnectionProvider.class).to(NamedQueryConnectionProviderImpl.class);
        requestStaticInjection(RDBMSConnectionProvider.class);
        requestStaticInjection(HBaseQuery.class);
        requestStaticInjection(RootDataMutator.class);
        requestStaticInjection(DataQueryServlet.class);
        requestStaticInjection(ResponseCacheFilter.class);
    }

//    @Provides
//    @Singleton
//    AppStateClient provideAppStateClient() throws UnknownHostException {
//        NamedQueryConfiguration config = NamedQueryConfiguration.getInstance();
//        Map<String, Map<String, Object>> appStateConfig = config.getAppState();
//        Map<String, Object> appStateStreamConfig = appStateConfig.get("stream");
//        Map<String, Object> appStateStorageConfig = appStateConfig.get("storage");
//        String hostName = InetAddress.getLocalHost().getHostName();
//        String applicationId = (String) appStateConfig.get("application").getOrDefault("name", "dataquery");
//        String groupIdSuffix = (String) appStateStreamConfig.getOrDefault("consumer_groupid_suffix", applicationId);
//        AppStateClientBuilder builder = AppStateClient.builder(config.getEnvironment(), applicationId, hostName + groupIdSuffix);
////        LOG.debug("Providing AppStateClient for applicationId {} and instanceId {}{}", applicationId, hostName, groupIdSuffix);
//        builder = builder.streamAdminConfig(appStateStreamConfig).defaultStorage();
//        if (StringUtils.equalsIgnoreCase(
//                (CharSequence) appStateStreamConfig.getOrDefault("type", "STREAM"),
//                "STREAM")) {
//            builder = builder.maprServers((String) appStateStreamConfig.get(DataqueryConstants.DATASTREAM_SERVER))
//                    .maprUser((String) appStateStreamConfig.get("user"))
//                    .maprPassword((String) appStateStreamConfig.get(DataqueryConstants.DATASTREAM_PASSWORD));
//        }
//        if (StringUtils.equalsIgnoreCase(
//                (CharSequence) appStateStorageConfig.getOrDefault("type", "ARANGODB"),
//                "ARANGODB")) {
//            builder = builder.storageHosts((String) appStateStorageConfig.get("hosts"))
//                    .storageSSL((Boolean) appStateStorageConfig.getOrDefault("useSSL", true))
//                    .storageUser((String) appStateStorageConfig.get("user"))
//                    .storagePassword((String) appStateStorageConfig.get(DataqueryConstants.DATASTREAM_PASSWORD));
//        } else {
//            builder = builder.inMemory();
//        }
//        return builder.buildDefaultClient();
//    }
}
