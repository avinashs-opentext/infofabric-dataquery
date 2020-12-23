/**
 * Copyright 2019 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.google.inject.Inject;
import com.liaison.dataquery.dto.AccessPrivileges;
import com.liaison.dataquery.dto.DmFilter;
import com.liaison.dataquery.dto.DmFilterset;
import com.liaison.dataquery.dto.DmPrivileges;
import com.liaison.dataquery.dto.DmRowPermission;
import com.liaison.dataquery.graphql.query.Filter;
import com.liaison.dataquery.graphql.query.FilterSet;
import com.opentext.infofabric.appstate.client.AppStateClient;
import com.opentext.infofabric.appstate.core.event.ErrorEvent;
import com.opentext.infofabric.appstate.core.event.StateEvent;
import com.opentext.infofabric.appstate.core.event.StateSubscription;
import com.opentext.infofabric.common.crypto.IFabricCryptoService;
import com.opentext.infofabric.registrar.stream.IFabricKafkaAdminClient;
import io.jsonwebtoken.lang.Collections;
import org.apache.kafka.clients.admin.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liaison.dataquery.exception.DataqueryException;
import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.NamedQueryConfiguration;
import com.liaison.dataquery.dto.DmModel;
import com.liaison.dataquery.dto.DmPermission;
import com.liaison.dataquery.dto.NamedQueryResponse;
import com.liaison.dataquery.dto.SqlResource;
import com.liaison.dataquery.exception.NamedQueryRuntimeException;
import com.liaison.dataquery.graphql.GraphQLService;
import com.liaison.dataquery.guice.GuiceInjector;
import com.liaison.dataquery.services.ModelService;
import com.liaison.dataquery.services.NamedQueryExecutorService;

public class AppStateService {
    private static final Logger log = LoggerFactory.getLogger(AppStateService.class);

    private static final String SCHEMA_VERSION = "V8";
    private static final String DM_PERMISSION_VERSION = "V3";
    private static final String DM_MODEL_VERSION = "V2";
    private final static String DEFAULT_BRANCH = "master";
    private static final String GET_ALL_TASKS = "FOR task IN sqlresource RETURN task";

    private static final String GET_TASKS = "FOR task IN sqlresource  FILTER task.fileName == @value  RETURN task";
    private static final String GET_DM_PERMISSION = "FOR DmPermission IN DM_PERMISSION RETURN DmPermission";
   
    private static AppStateClient appStateClient = GuiceInjector.getInjector().getInstance(AppStateClient.class);
    private static ModelService modelService = GuiceInjector.getInjector().getInstance(ModelService.class);
    private static DataqueryConfiguration configuration = GuiceInjector.getInjector().getInstance(DataqueryConfiguration.class);
    private static GraphQLService graphQLService = GuiceInjector.getInjector().getInstance(GraphQLService.class);
    
    private static NamedQueryExecutorService namedQueryExecutorService = GuiceInjector.getInjector().getInstance(NamedQueryExecutorService.class);
    private static ConcurrentHashMap<String, DmPermission> dmPermissionCache = new ConcurrentHashMap<String, DmPermission>();

    @Inject
    private static IFabricCryptoService cryptoService;

    public static void callConsumerRequest() {
        consumerRequest(processJobResponse, processSqlResource, processDmPermission,processDmModel, processError, StateSubscription.OffsetStrategy.CONTINUE);
    }

    private static void consumerRequest(Function<StateEvent<NamedQueryResponse>, Boolean> callback,
                                        Function<StateEvent<SqlResource>, Boolean> callback_sql,
                                        Function<StateEvent<DmPermission>, Boolean> callback_dmPermission,
                                        Function<StateEvent<DmModel>, Boolean> callback_dmModel,
                                        Function<ErrorEvent, Boolean> errorCallback,
                                        StateSubscription.OffsetStrategy offsetStrategy) {
        try {
            List<StateSubscription> subs = new ArrayList<>();
            StateSubscription sub = new StateSubscription<>(DataqueryConstants.COLLECTION, NamedQueryResponse.class)
                    .setOffsetStrategy(offsetStrategy)
                    .setFilterOwn(false)
                    .setSuccessCallback(callback)
                    .setErrorCallback(errorCallback);
            StateSubscription sub2 = new StateSubscription<>(DataqueryConstants.SQL_RESOURCE, SqlResource.class)
                    .setOffsetStrategy(offsetStrategy)
                    .setFilterOwn(false)
                    .setSuccessCallback(callback_sql)
                    .setErrorCallback(errorCallback);
            StateSubscription sub3 = new StateSubscription<>(DataqueryConstants.DM_PERMISSION,
                    DmPermission.class).setOffsetStrategy(offsetStrategy).setFilterOwn(false)
                    .setSuccessCallback(callback_dmPermission).setErrorCallback(errorCallback);
            StateSubscription sub4 = new StateSubscription<>(DataqueryConstants.DM_MODEL,
                    DmModel.class).setOffsetStrategy(offsetStrategy).setFilterOwn(false)
                    .setSuccessCallback(callback_dmModel).setErrorCallback(errorCallback);



            subs.add(sub);
            subs.add(sub2);
            subs.add(sub3);
            subs.add(sub4);
            log.info("registering listeners on " + DataqueryConstants.COLLECTION + " with SCHEMA VERSION : " + SCHEMA_VERSION);
            appStateClient.ensureSchema(DataqueryConstants.COLLECTION, NamedQueryResponse.class, SCHEMA_VERSION);
            log.info("registering listeners on " + DataqueryConstants.SQL_RESOURCE + " with SCHEMA VERSION : " + SCHEMA_VERSION);
            appStateClient.ensureSchema(DataqueryConstants.SQL_RESOURCE, SqlResource.class, SCHEMA_VERSION);
            log.info("registering listeners on " + DataqueryConstants.DM_PERMISSION + " with SCHEMA VERSION : " + DM_PERMISSION_VERSION);
            appStateClient.ensureSchema(DataqueryConstants.DM_PERMISSION, DmPermission.class, DM_PERMISSION_VERSION);
            log.info("registering listeners on " + DataqueryConstants.DM_MODEL + " with SCHEMA VERSION : " + DM_MODEL_VERSION);
            appStateClient.ensureSchema(DataqueryConstants.DM_MODEL, DmModel.class, DM_MODEL_VERSION);
            appStateClient.subscribe(subs);
        } catch (Exception sne) {
            log.error("exceptions when subscribing to command stream.", sne);
            throw new NamedQueryRuntimeException("exceptions when subscribing to command stream.", sne);
        }
    }

    public static void writeTaskToStream(final NamedQueryResponse response) {
        appStateClient.update(DataqueryConstants.COLLECTION, response.getJobId(), response);
    }
    
	/*
	 * public static void writeDmModel(DmModel dmModel, String key) {
	 * appStateClient.update(DataqueryConstants.DM_MODEL, key, dmModel); }
	 */

    private static Function<ErrorEvent, Boolean> processError = (event) -> {
        //Some exception occurred during data serialization among storage and stream in appstate
        //nothing can be done here and just log the event details for information.
        String evenDetails = String.format(
                "[task name=%s, topic=%s, partition=%s, offset=%s, collection=%s, command=%s, message=%s]",
                event.getKey(),
                event.getTopic(),
                event.getPartition(),
                event.getOffset(),
                event.getCollection(),
                event.getCommand().name(),
                event.getErrorMessage());
        log.error(evenDetails, event.getException());
        return true;
    };

    private static Function<StateEvent<NamedQueryResponse>, Boolean> processJobResponse = (event) -> {
        log.debug("Received the state event for NamedQueryResponse");

        try {
            if (null == event || null == event.getData()) {
                log.warn("The state event or task wrapper is null.");
                return true;
            }
            NamedQueryResponse response = event.getData();
            namedQueryExecutorService.setJobIdStatus(response);
        } catch (final Throwable e) {
            log.error("Issue processing tasks from stream.", e);
        }
        return true;

    };

    private static Function<StateEvent<SqlResource>, Boolean> processSqlResource = (event) -> {
        log.info("Received the state event for SqlResource");

        try {
            if (null == event || null == event.getData()) {
                log.warn("The state event or task wrapper is null.");
                return true;
            }
            SqlResource response = event.getData();
            namedQueryExecutorService.updateMap(response);
        } catch (final Throwable e) {
            log.error("Issue processing tasks from stream.", e);
        }
        return true;

    };

    private static Function<StateEvent<DmPermission>, Boolean> processDmPermission = (event) -> {
        log.info("Received the callback event for processDmPermission");
        try {
            if (null == event || null == event.getData()) {
                log.warn("The state event or task wrapper is null.");
                return true;
            }
            DmPermission response = event.getData();
            dmPermissionCache.put(response.get_key(), response);
            if (log.isDebugEnabled()) {
                log.debug("updated dmPermissionCache::: " + dmPermissionCache.toString());
            }
        } catch (final Throwable e) {
            log.error("Issue processing tasks from stream.", e);
        }
        return true;
    };
    
	private static Function<StateEvent<DmModel>, Boolean> processDmModel = (event) -> {
		try {
			if (null == event || null == event.getData()) {
				log.warn("The state event or task wrapper is null.");
				return true;
			}
			DmModel response = event.getData();
			String authToken = getAuthToken();
			log.info("Received the callback event for refresh model with Tenant Name: " + response.getTenant()
					+ " Model: " + response.getModel());
			String sdl = modelService.getSDLByModel(authToken, response.getTenant(), response.getModel(), null, null);
			graphQLService.createService(response.getTenant(), response.getModel(), DEFAULT_BRANCH, sdl);
		} catch (final Throwable e) {
			log.error("Issue processing tasks from stream.", e);
		}
		return true;
	};

    public static boolean doesTopicExist(String tenant, String topicName) {
        IFabricKafkaAdminClient admin = GuiceInjector.getInjector().getInstance(IFabricKafkaAdminClient.class);
        String path = String.format(NamedQueryConfiguration.getInstance().getDataStreamPath(), tenant, topicName);
        return admin.doesTopicExist(path);
    }

    public static void writeSql(String sqlfileName, SqlResource obj) {
        appStateClient.update("sqlresource", sqlfileName, obj);
    }

    public static SqlResource getSqlResource(String name) {
        Collection<SqlResource> returnCol = null;
        HashMap<String, Object> params = new HashMap<>();
        params.put("value", name);
        returnCol = appStateClient.query(DataqueryUtils.getApplicationId(), GET_TASKS, params, SqlResource.class);
        if (null != returnCol && returnCol.iterator().hasNext()) {
            return returnCol.iterator().next();
        } else {
            return null;
        }
    }

    public static Collection<SqlResource> getSqlResources() {
        Collection<SqlResource> returnCol = null;
        HashMap<String, Object> params = new HashMap<>();
        returnCol = appStateClient.query(DataqueryUtils.getApplicationId(), GET_ALL_TASKS, params, SqlResource.class);
        return returnCol;
    }

    public static void buildDMPermissionCache() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        Collection<DmPermission> results = appStateClient.query(DataqueryUtils.getApplicationId(), GET_DM_PERMISSION,
                params, DmPermission.class);
        results.forEach(dmPermission -> dmPermissionCache.put(dmPermission.get_key(), dmPermission));
        if (log.isDebugEnabled()) {
            log.debug("dmPermissionCache::: " + dmPermissionCache.toString());
        }
    }

    /**
     * This method return the valid model types based on DM roles
     *
     * @param ssoRoles  - list of sso roles for the user
     * @param tenant    - tenant
     * @param model     - model
     * @param operation - operation read/ write/ insert/ upsert
     * @return - set of types
     */
    public static AccessPrivileges getTablePrivilegesFromCache(List<String> ssoRoles, String tenant, String model) {
        return DataqueryUtils.getaccessPrivileges( ssoRoles,  tenant,  model,  dmPermissionCache);
    }

    public static void addPermissionsForTest(String tenant, String ssoRole, DmPermission permission) {
        dmPermissionCache.put(tenant + "_" + ssoRole, permission);
    }
    
	private static String getAuthToken() throws DataqueryException {
		try {
			return modelService.getAuthToken((String) configuration.getDmModelUsername(), cryptoService.getDecryptedData((String) configuration.getDmModelPassword())
					.toCharArray());
		} catch (Exception e) {
			throw new DataqueryException(
					"Exception trying to retrieve authentication token: " + e.getLocalizedMessage(), e);
		}
	}
}
