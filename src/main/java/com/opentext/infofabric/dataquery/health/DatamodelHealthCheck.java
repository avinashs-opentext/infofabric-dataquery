/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.health;

import com.google.inject.Inject;
import com.opentext.infofabric.dataquery.DataqueryConfiguration;
import com.opentext.infofabric.dataquery.exception.DataqueryException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;

public class DatamodelHealthCheck extends BaseHealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(DatamodelHealthCheck.class);

    private static final String DATAMODEL_PATH = "/2/datamodel/health?simple=true";

    protected CloseableHttpClient httpClient;

    private DataqueryConfiguration dataqueryConfiguration;

	@Inject
	public DatamodelHealthCheck(CloseableHttpClient httpClient, DataqueryConfiguration dataqueryConfiguration) {
		this.httpClient = httpClient;
		this.dataqueryConfiguration = dataqueryConfiguration;
	}

    @Override
    protected Result performHealthCheck() throws Exception {
        String dataModelUrl = dataqueryConfiguration.getDataModelBaseUrl().concat(DATAMODEL_PATH);
        HttpGet httpGet = new HttpGet(dataModelUrl);
        try (
             CloseableHttpResponse response = httpClient.execute(httpGet);
             InputStream is = response.getEntity().getContent()) {
            String responseJson = IOUtils.toString(is, Charset.forName("UTF-8"));
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                String errorMessage = String.format("DataModel health check failed : HTTP error code : %s ", response.getStatusLine().getStatusCode());
                logger.warn("health check response from DataModel service is {}", responseJson);
                throw new DataqueryException(errorMessage);
            }
        }
        return Result.healthy();
    }
}
