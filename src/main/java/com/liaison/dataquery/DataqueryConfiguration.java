/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import com.liaison.dataquery.dto.MetricReporterConfiguration;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.graphql.RootDataAccess;
import com.opentext.infofabric.common.crypto.IFabricCryptoService;
import com.opentext.infofabric.registrar.types.DataCenterEnvironment;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Dataquery Configuration
 */
public class DataqueryConfiguration extends Configuration {


    private static final Logger logger = LoggerFactory.getLogger(DataqueryConfiguration.class);

    @Inject
    private IFabricCryptoService cryptoService;

    @JsonProperty
    private String symmetricKeyPath;

    @JsonProperty
    private DataCenterEnvironment environment;

    @JsonProperty
    private MetricReporterConfiguration metricReporter;

    @JsonProperty("ssoClient")
    private Map<String, Object> ssoClient;

    @JsonProperty
    private boolean enableSSO;

    @JsonProperty
    private Integer maxGraphQLQueryComplexity;

    @JsonProperty
    private Integer maxJoinFieldComplexity;

    @com.fasterxml.jackson.annotation.JsonProperty
    private Boolean mockAuth;

    @JsonProperty
    private Boolean disableAdminContextAuth;

    @JsonProperty
    private String httpClientTrustStorePath;

    @JsonProperty
    private String httpClientTrustStorePassword;

    @JsonProperty("loadBalancer")
    private Map<String, Object> loadBalancerConfig;

    @JsonProperty("maprAdminClientConfig")
    private Map<String, Object> maprAdminClientConfig;

    @JsonProperty("kafkaAdminClient")
    private Map<String, Object> kafkaAdminClient;

    @JsonProperty("rdbms")
    private Map<String, Object> rdbms;

    @JsonProperty("maxCacheEntryBytes")
    private Long maxCacheEntryBytes = 100 * 1000L; //Default to 100kb per cache entry

    @JsonProperty("cacheTTLMs")
    private Long cacheTTLMs = 60 * 1000L; //Default to 60s

    @JsonProperty("cacheSize")
    private Long cacheSize = 1000L; //Default to 1000 entries per cache

    @JsonProperty("recordCacheStats")
    private boolean recordCacheStats;

    @com.fasterxml.jackson.annotation.JsonProperty("httpClient")
    private HttpClientConfiguration httpClientConfiguration = new HttpClientConfiguration();

    @JsonProperty("defaultQueryModel")
    private RootDataAccess.QueryModel defaultQueryModel;

    @JsonProperty("appState")
    private Map<String, Map<String, Object>> appState;

    @JsonProperty("admin")
    private Map<String, Object> admin;

    @JsonProperty
    private DataStreamConfiguration datastream;

    @JsonProperty("httpRequestConfig")
    private HashMap<String, Object> httpRequestConfig;

    @JsonProperty
    private String streamImplementation;

    @JsonProperty
    private String dmServiceToken;

    @JsonProperty
    private String datacastUrl;

    public DataStreamConfiguration getDatastream() {
        return datastream;
    }

    public void setDatastream(DataStreamConfiguration datastream) {
        this.datastream = datastream;
    }


    public Map<String, Map<String, Object>> getAppState() {
        if (appState != null && symmetricKeyPath!= null ) {
            appState.get("stream").computeIfPresent("password", decryptFunction);
            appState.get("storage").computeIfPresent("password", decryptFunction);
        }
        return appState;
    }

    public void setAppState(Map<String, Map<String, Object>> appState) {
        this.appState = appState;
    }

    public Map<String, Object> getAdmin() {
        if (admin != null) {
            admin.computeIfPresent("pass", decryptFunction);
        }
        return admin;
    }

    public void setAdmin(Map<String, Object> admin) {
        this.admin = admin;
    }

    public Boolean getMockAuth() {
        return mockAuth;
    }

    public void setMockAuth(Boolean mockAuth) {
        this.mockAuth = mockAuth;
    }

    public Map<String, Object> getLoadBalancerConfig() {
        return loadBalancerConfig;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("datamodel")
    private Map<String, Object> datamodelConfig;

    public Map<String, Object> getDatamodelConfig() {
        return datamodelConfig;
    }

    public String getDataModelBaseUrl() {
        Map<String, Object> datamodelConfig = getDatamodelConfig();
        return datamodelConfig.get("base-url").toString();
    }

    public String getDmModelUsername() {
        Map<String, Object> datamodelConfig = getDatamodelConfig();
        return datamodelConfig.get("user").toString();
    }
    
    public String getDmModelPassword() {
        Map<String, Object> datamodelConfig = getDatamodelConfig();
        return datamodelConfig.get("password").toString();
    }


    public void setDatamodelConfig(Map<String, Object> datamodelConfig) {
        this.datamodelConfig = datamodelConfig;
    }


    public void setLoadBalancerConfig(Map<String, Object> loadBalancerConfig) {
        this.loadBalancerConfig = loadBalancerConfig;
    }

    public String getHttpClientTrustStorePath() {
        return httpClientTrustStorePath;
    }

    public void setHttpClientTrustStorePath(String httpClientTrustStorePath) {
        this.httpClientTrustStorePath = httpClientTrustStorePath;
    }

    public String getHttpClientTrustStorePassword() {
        return httpClientTrustStorePassword;
    }

    public void setHttpClientTrustStorePassword(String httpClientTrustStorePassword) {
        this.httpClientTrustStorePassword = httpClientTrustStorePassword;
    }

    public String getSymmetricKeyPath() {
        return symmetricKeyPath;
    }

    public DataqueryConfiguration setSymmetricKeyPath(String symmetricKeyPath) {
        this.symmetricKeyPath = symmetricKeyPath;
        return this;
    }

    public DataCenterEnvironment getEnvironment() {
        return environment;
    }

    public DataqueryConfiguration setEnvironment(DataCenterEnvironment environment) {
        this.environment = environment;
        return this;
    }
    
    public MetricReporterConfiguration getMetricReporter() {
        return metricReporter;
    }

    public void setMetricReporter(MetricReporterConfiguration metricReporter) {
        this.metricReporter = metricReporter;
    }

    public Map<String, Object> getSsoClient() {
        return ssoClient;
    }

    public void setSsoClient(Map<String, Object> ssoClient) {
        this.ssoClient = ssoClient;
    }

    public boolean isEnableSSO() {
        return enableSSO;
    }

    public void setEnableSSO(boolean enableSSO) {
        this.enableSSO = enableSSO;
    }

    public Integer getMaxGraphQLQueryComplexity() {
        return maxGraphQLQueryComplexity;
    }

    public void setMaxGraphQLQueryComplexity(Integer maxGraphQLQueryComplexity) {
        this.maxGraphQLQueryComplexity = maxGraphQLQueryComplexity;
    }

    public Integer getMaxJoinFieldComplexity() {
        return maxJoinFieldComplexity;
    }

    public void setMaxJoinFieldComplexity(Integer maxJoinFieldComplexity) {
        this.maxJoinFieldComplexity = maxJoinFieldComplexity;
    }

    public Boolean getDisableAdminContextAuth() {
        return disableAdminContextAuth;
    }

    public void setDisableAdminContextAuth(Boolean disableAdminContextAuth) {
        this.disableAdminContextAuth = disableAdminContextAuth;
    }

    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClientConfiguration;
    }

    public void setHttpClientConfiguration(HttpClientConfiguration httpClientConfiguration) {
        this.httpClientConfiguration = httpClientConfiguration;
    }

    private BiFunction<String, Object, Object> decryptFunction = (k, v) -> getDecryptedToken(k, (String) v);

    public Map<String, Object> getDecryptedASsoClientConfig() {
        if (ssoClient != null && symmetricKeyPath != null) {
            ssoClient.computeIfPresent("clientSecret", decryptFunction);
        }
        return ssoClient;
    }

    public Map<String, Object> getDecryptedMaprAdminConfig() {
        if (maprAdminClientConfig != null && symmetricKeyPath != null) {
            maprAdminClientConfig.computeIfPresent("pass", decryptFunction);
        }
        return maprAdminClientConfig;
    }

    public Map<String, Object> getDecryptedRdbmsConfig() {
        if (rdbms != null && symmetricKeyPath != null) {
            rdbms.computeIfPresent("password", decryptFunction);
        }
        return rdbms;
    }

    public String getDecryptedToken(String key, String encryptedToken) {
        try {
            return cryptoService.getDecryptedData(encryptedToken);
        } catch (Throwable e) {
            logger.error("Exception in getDecryptedToken for the key : "+key);
            throw new DataqueryRuntimeException("Exception in getDecryptedToken", e);
        }
    }

    public Map<String, Object> getMaprAdminClientConfig() {
        return maprAdminClientConfig;
    }

    public void setMaprAdminClientConfig(Map<String, Object> maprAdminClientConfig) {
        this.maprAdminClientConfig = maprAdminClientConfig;
    }

    public Map<String, Object> getRdbms() {
        return rdbms;
    }

    public Long getMaxCacheEntryBytes() {
        return maxCacheEntryBytes;
    }

    public Long getCacheTTLMs() {
        return cacheTTLMs;
    }

    public Long getCacheSize() {
        return cacheSize;
    }

    public boolean getRecordCacheStats() {
        return recordCacheStats;
    }

    public RootDataAccess.QueryModel getDefaultQueryModel() {
        return defaultQueryModel;
    }

    public HashMap<String, Object> getHttpRequestConfig() {
        return httpRequestConfig;
    }

    public void setHttpRequestConfig(HashMap<String, Object> httpRequestConfig) {
        this.httpRequestConfig = httpRequestConfig;
    }

    public String getDmServiceToken() {
        return dmServiceToken;
    }

    public void setDmServiceToken(String dmServiceToken) {
        this.dmServiceToken = dmServiceToken;
    }

    public String getDatacastUrl() {
        return datacastUrl;
    }

    public void setDatacastUrl(String datacastUrl) {
        this.datacastUrl = datacastUrl;
    }

    public String getStreamImplementation() {
        return streamImplementation;
    }

    public void setStreamImplementation(String streamImplementation) {
        this.streamImplementation = streamImplementation;
    }

    public Map<String, Object> getKafkaAdminClient() {
        return kafkaAdminClient;
    }

    public void setKafkaAdminClient(Map<String, Object> kafkaAdminClient) {
        this.kafkaAdminClient = kafkaAdminClient;
    }

    @JsonProperty("registrarClient")
    private Map<String, Object> registrarClient;

    public Map<String, Object> getRegistrarClient() {
        if (!registrarClient.containsKey(streamImplementation)) {
            throw new RuntimeException("Missing stream path for Registrar client " + streamImplementation);
        }
        return (Map<String, Object>) registrarClient.get(streamImplementation);
    }

    public void setRegistrarClient(Map<String, Object> registrarClient) {
        this.registrarClient = registrarClient;
    }
}
