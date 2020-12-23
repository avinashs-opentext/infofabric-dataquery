/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.mutation;

import java.util.HashMap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.exception.DataCastManagerException;
import com.liaison.dataquery.graphql.RootDataAccess;
import com.liaison.dataquery.graphql.query.QueryContext;
import com.liaison.dataquery.graphql.results.ResultList;
import com.liaison.dataquery.graphql.results.ResultObject;
import com.liaison.dataquery.services.impl.BaseManagerImpl;

public class DatacastTransactionServiceImpl extends BaseManagerImpl implements TransactionService {

    private static final String SUFFIX_DATA_QUERY_HTTP_CONNECTOR = "_http_dq";

    @Inject
    private static TransactionService dataCastManagerImpl;

    private Gson gson = new Gson() ;

	@Inject
	public DatacastTransactionServiceImpl(CloseableHttpClient httpClient, DataqueryConfiguration configuration) {
		super(httpClient, configuration);
	}

    public DatacastTransactionServiceImpl() {
        super();
    }

    @Override
    public ResultList upsert(QueryContext context, byte[] inputJson) throws DataCastManagerException {
        String connectorName = getHttpConnectorName(context);
        ResultList resultList = new ResultList();
        JSONObject connectorResponse;
        try {
            String connectorUrl = configuration.getDatacastUrl().concat(context.getTenant()).concat(DataqueryConstants.DATA_PATH).concat(connectorName);
            if (context.getQueryType().equals(RootDataAccess.QueryType.INSERT)) {
                connectorResponse = executePost(context.getToken(), configuration.getDmServiceToken(), connectorUrl, inputJson, "dq_insert", false);
            } else {
                connectorResponse = executePut(context.getToken(), configuration.getDmServiceToken(), connectorUrl, inputJson, "dq_upsert", false);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Data cast mutation response :" + connectorResponse);
            }
            HashMap<String, Object> hashMap = gson.fromJson(connectorResponse.toString(), HashMap.class);
            ResultObject resultMap = new ResultObject(hashMap);
            resultList.add(resultMap);

            return resultList;
        } catch (Exception e) {
            logger.error("Exception in postToConnector", e);
            String message = String.format("Exception occured while posting data to connector. Exception: %s", e.getMessage());
            throw new DataCastManagerException(message);
        }
    }

    private String getHttpConnectorName(QueryContext context) {
        StringBuilder str = new StringBuilder();
        str.append(context.getTenant())
            .append(DataqueryConstants.UNDERSCORE)
            .append(context.getDatamodel())
            .append(SUFFIX_DATA_QUERY_HTTP_CONNECTOR);
        return str.toString();
    }
}
