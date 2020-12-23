/**
 * Copyright 2019 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.health;

import com.google.inject.Inject;
import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.exception.DataqueryException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created by vkukkadapu on 4/23/19.
 */
public class DataCastHealthCheck extends BaseHealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(DataCastHealthCheck.class);

    private static final String DATACAST_PATH = "health?simple=true";

    protected CloseableHttpClient httpClient;

    private DataqueryConfiguration dataqueryConfiguration;

    @Inject
    public DataCastHealthCheck(CloseableHttpClient httpClient, DataqueryConfiguration dataqueryConfiguration) {
        this.httpClient = httpClient;
        this.dataqueryConfiguration = dataqueryConfiguration;
    }

    @Override
    protected Result performHealthCheck() throws Exception {
        String dataCastUrl = dataqueryConfiguration.getDatacastUrl().concat(DATACAST_PATH);
        HttpGet httpGet = new HttpGet(dataCastUrl);
        try (
             CloseableHttpResponse response = httpClient.execute(httpGet);
             InputStream is = response.getEntity().getContent()) {
            String responseJson = IOUtils.toString(is, Charset.forName("UTF-8"));
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                String errorMessage = String.format("DataCast health check failed : HTTP error code : %s ", response.getStatusLine().getStatusCode());
                logger.warn("health check response from DataCast service is {}", responseJson);
                throw new DataqueryException(errorMessage);
            }
        }
        return Result.healthy();
    }

}
