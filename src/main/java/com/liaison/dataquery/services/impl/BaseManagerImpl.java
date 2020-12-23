/**
 * Copyright 2019 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by vkukkadapu on 3/25/19.
 */
public class BaseManagerImpl {
    protected static final Logger logger = LoggerFactory.getLogger(BaseManagerImpl.class);
    private static RequestConfig defaultRequestConfig;
    private static int socketTimeout;
    private static int connectionTimeout;
    private static int connectionRequestTimeout;

    static {
        socketTimeout = 120000;
        connectionTimeout = 120000;
        connectionRequestTimeout = 120000;
    }

    protected CloseableHttpClient httpClient;
    @Inject
    protected DataqueryConfiguration configuration;

    public BaseManagerImpl( CloseableHttpClient httpClient, DataqueryConfiguration configuration) {
        this.httpClient = httpClient;
        this.configuration = configuration;
        try {
            if (configuration.getHttpRequestConfig() != null) {
                if (configuration.getHttpRequestConfig().containsKey(DataqueryConstants.SOCKET_TIMEOUT)) {
                    socketTimeout = Integer.parseInt(configuration.getHttpRequestConfig().get(DataqueryConstants.SOCKET_TIMEOUT).toString());
                }
                if (configuration.getHttpRequestConfig().containsKey(DataqueryConstants.CONNECTION_TIMEOUT)) {
                    connectionTimeout = Integer.parseInt(configuration.getHttpRequestConfig().get(DataqueryConstants.CONNECTION_TIMEOUT).toString());
                }
                if (configuration.getHttpRequestConfig().containsKey(DataqueryConstants.CONNECTION_REQUEST_TIMEOUT)) {
                    connectionRequestTimeout = Integer.parseInt(configuration.getHttpRequestConfig().get(DataqueryConstants.CONNECTION_REQUEST_TIMEOUT).toString());
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("One of sockerTimeout, connectionTimeout or connectionRequestTimeout is not an integer", e);
        }
        defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .build();
    }

    public BaseManagerImpl() {

    }

    public static RequestConfig getDefaultRequestConfig() {
        return defaultRequestConfig;
    }

    protected JSONObject executePost(String authToken, String dmServiceToken, String url, String inputJson, String methodName, boolean logResponse) throws IOException {
        StringEntity stringEntity = new StringEntity(inputJson, "UTF-8");
        return postHttpEntity(authToken, dmServiceToken, url, stringEntity, methodName, logResponse);
    }

    private JSONObject postHttpEntity(String authToken, String dmServiceToken, String url, AbstractHttpEntity httpEntity, String methodName, boolean logResponse) throws IOException {
        HttpPost post = new HttpPost(url);
        setHeadersAndConfig(post, authToken, dmServiceToken);
        post.setEntity(httpEntity);
        return executeRequest(post, methodName, logResponse);
    }

    protected JSONObject executePost(String authToken, String dmServiceToken, String url, byte[] inputJson, String methodName, boolean logResponse) throws IOException {
        ByteArrayEntity byteArrayEntity = new ByteArrayEntity(inputJson);
        return postHttpEntity(authToken, dmServiceToken, url, byteArrayEntity, methodName, logResponse);
    }

    protected JSONObject executeGet(String authToken, String url, String methodName, boolean logResponse) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        setHeadersAndConfig(httpGet, authToken, null);
        return executeRequest(httpGet, methodName, logResponse);
    }

    protected JSONObject executePut(String authToken, String dmServiceToken, String url, byte[] inputJson, String methodName, boolean logResponse) throws IOException {
        ByteArrayEntity byteArrayEntity = new ByteArrayEntity(inputJson);
        return putHttpEntity(authToken, dmServiceToken, url, byteArrayEntity, methodName, logResponse);
    }

    private JSONObject putHttpEntity(String authToken, String dmServiceToken, String url, AbstractHttpEntity byteArrayEntity, String methodName, boolean logResponse) throws IOException {
        HttpPut put = new HttpPut(url);
        setHeadersAndConfig(put, authToken, dmServiceToken);
        put.setEntity(byteArrayEntity);
        return executeRequest(put, methodName, logResponse);
    }

    private JSONObject executeRequest(HttpRequestBase httpType, String methodName, boolean logResponse) throws IOException {
        try (
             CloseableHttpResponse response = httpClient.execute(httpType);
             InputStream is = response.getEntity().getContent()) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> dcmap = mapper.readValue(is, Map.class);
            JSONObject responseJson = new JSONObject(dcmap);
            //String responseJson = IOUtils.toString(is, Charset.forName("UTF-8"));
            if (logResponse) {
                logger.info("Response JSON is : {}", responseJson);
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                String errorMessage = String.format("%s failed : HTTP error code : %s : %s", methodName, response.getStatusLine().getStatusCode(), getMessageFromJsonResponse(responseJson.toString()));
                logger.warn(errorMessage);
                throw new DataqueryRuntimeException(errorMessage);
            }
            return responseJson;
        }
    }

    private void setHeadersAndConfig(HttpRequestBase httpRequestBase, String authToken, String dmServiceToken) {
        httpRequestBase.setConfig(getDefaultRequestConfig());
        httpRequestBase.addHeader(DataqueryConstants.CONTENT_TYPE, DataqueryConstants.APPLICATION_JSON_CONTENT);
        if (StringUtils.isNotEmpty(authToken)) {
            httpRequestBase.addHeader(DataqueryConstants.AUTH_HEADER, "Bearer " + authToken);
        }
        if (StringUtils.isNotEmpty(dmServiceToken)) {
            httpRequestBase.addHeader(DataqueryConstants.X_SERVICE_TOKEN, dmServiceToken);
        }
    }

    protected String getMessageFromJsonResponse(String responseJson) {
        JsonObject jsonObject = new Gson().fromJson(responseJson, JsonObject.class);
        return jsonObject.get("message").getAsString();
    }

    protected String getResultsFromJsonResponse(String responseJson) {
        JsonObject jsonObject = new Gson().fromJson(responseJson, JsonObject.class);
        if (jsonObject.get("results") instanceof JsonObject)
            return jsonObject.get("results").toString();
        else {
            return jsonObject.get("results").getAsString();
        }
    }

}
