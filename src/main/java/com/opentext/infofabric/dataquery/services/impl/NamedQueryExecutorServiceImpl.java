/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.services.impl;


import com.google.inject.Inject;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.dto.NamedQueryApiResponse;
import com.opentext.infofabric.dataquery.dto.NamedQueryRequest;
import com.opentext.infofabric.dataquery.dto.NamedQueryResponse;
import com.opentext.infofabric.dataquery.dto.NamedQueryVariable;
import com.opentext.infofabric.dataquery.dto.SqlResource;
import com.opentext.infofabric.dataquery.exception.NamedQueryRuntimeException;
import com.opentext.infofabric.dataquery.namedqueries.rdbms.NamedPreparedStatement;
import com.opentext.infofabric.dataquery.services.NamedQueryConnectionFactory;
import com.opentext.infofabric.dataquery.services.NamedQueryExecutorService;
import com.opentext.infofabric.dataquery.util.AppStateService;
import com.opentext.infofabric.dataquery.util.ResultSetStreamUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamedQueryExecutorServiceImpl implements NamedQueryExecutorService {

    private static final Logger log = LoggerFactory.getLogger(NamedQueryExecutorServiceImpl.class);

    private enum jobStatus {starting, executing, executed, writing, success}

    Map<String, NamedQueryResponse> namedQueryResponseMap = new Hashtable<String, NamedQueryResponse>();

    private Map<String, SqlResource> queryMap = new HashMap<>();

//    private NamedQueryConfiguration namedQueryConfig = NamedQueryConfiguration.getInstance();

    public NamedQueryExecutorServiceImpl() {}

    @Inject
    ResultSetStreamUtil resultSetStreamUtil;

    @Override
    public Set<String> getNames() {
        if(queryMap.isEmpty()){
            log.info("Empty map, populating it");
            Collection<SqlResource> queryList = AppStateService.getSqlResources();
            queryList.forEach( query -> {
                queryMap.put(query.getFileName(),query);
            });
        }
        return queryMap.keySet();
    }

    @Override
    public void executeQuery(String tenant, String datamodel, NamedQueryRequest request, AsyncResponse response) {
        String queryName = request.getQuery();
        List<NamedQueryVariable> variables = request.getVariables();
        String streamName = request.getStreamName();
        String typeName = request.getTargetType();
        new Thread() {
            public void run() {

                Connection connection = null;
                NamedPreparedStatement preparedStatement = null;
                try {
                String query = getQuery(queryName);
                log.info("Executing query "+query);
                final AtomicBoolean timeout = new AtomicBoolean(false);
                final String jobID = UUID.randomUUID().toString();
                NamedQueryResponse res = new NamedQueryResponse();
                res.setJobId(jobID);
                res.setJobStatus(jobStatus.starting.toString());
                response.setTimeout(DataqueryConstants.ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                response.setTimeoutHandler(new TimeoutHandler() {
                                               @Override
                                               public void handleTimeout(AsyncResponse asyncResponse) {
                                                   timeout.set(true);
                                                   namedQueryResponseMap.put(jobID, res);
                                                   AppStateService.writeTaskToStream(res);
                                                   response.resume(Response.ok(new NamedQueryApiResponse().setStatus(DataqueryConstants.SUCCESS).setResult(namedQueryResponseMap.get(jobID))).build());
                                               }
                                           }
                );

               /* try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                    Map<String, Object> previewMap = preview(tenant, datamodel, query, variables);
                    connection = (Connection) previewMap.get("connection");
                    preparedStatement = (NamedPreparedStatement) previewMap.get("preparedStatement");
                    res.setJobStatus(jobStatus.executing.toString());

                    boolean result = preparedStatement.getPreparedStatement().execute();
                    res.setJobStatus(jobStatus.executed.toString());

                    int resultCount = 0;
                    JSONArray results = null;
                    if (result) { // result set found
                        if (StringUtils.isEmpty(request.getStreamName())) {
                            res.setJobStatus(jobStatus.writing.toString());
                            JSONArray jsonArray = resultSetStreamUtil.getSyncResponse(preparedStatement.getPreparedStatement().getResultSet());
                            res.setResultJson(jsonArray.toString());
                            res.setResultCount(jsonArray.length());
                        } else {
                            if (!AppStateService.doesTopicExist(tenant, request.getStreamName())) {
                                throw new NamedQueryRuntimeException(String.format(DataqueryConstants.STREAM_NOT_EXIST, tenant, request.getStreamName()));
                            }
                            res.setStreamName(streamName);
                            res.setJobStatus(jobStatus.writing.toString());
                            res.setResultCount(resultSetStreamUtil.stream(preparedStatement.getPreparedStatement().getResultSet(), jobID, streamName, typeName, tenant));
                        }
                    } else {
                        resultCount = preparedStatement.getPreparedStatement().getUpdateCount();
                        res.setResultCount(resultCount);
                    }
                    res.setJobStatus(jobStatus.success.toString());
                    res.setJobMessage("successfully executed");
                    log.info("successfully executed job with jobID : "+jobID);
                    if (timeout.get()) {
                        AppStateService.writeTaskToStream(res);
                    }
                    response.resume(Response.ok(new NamedQueryApiResponse().setStatus(DataqueryConstants.SUCCESS).setResult(res)).build());
                } catch (Exception e) {
                    log.error("Error in executing", e);
                    response.resume( Response.ok(new NamedQueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(e.getMessage())).build());
                    throw new NamedQueryRuntimeException(e);
                } finally {
                    if (preparedStatement != null) {
                        try {
                            preparedStatement.getPreparedStatement().close();
                        } catch (SQLException e) {
                            log.error("Error closing prepared statement.", e);
                        }
                    }
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (SQLException e) {
                            log.error("Error closing connection.", e);
                        }
                    }
                }
            }
        }.start();

    }

    private Map<String, Object> preview(String tenant, String model, String queryToBeExecuted, List<NamedQueryVariable> variables) throws SQLException {
        Matcher matcher = Pattern.compile("^.*(\\\"(.*?)\\\").*$").matcher(queryToBeExecuted);
        if(matcher.find()){
            String queryModelName = matcher.group(2);
            if(!queryModelName.equalsIgnoreCase(model)){
                throw new NamedQueryRuntimeException("Model Name provided in the query does not match the Model name provided in the request : "+ model+" != "+queryModelName);
            }
        } else {
            throw new NamedQueryRuntimeException("query does not have a Model Name provided in the right format.");
        }

        log.info("Preview query "+queryToBeExecuted);
        Map<String, Object> returnMap = new HashMap<>();
        Connection connection = null;
        NamedPreparedStatement preparedStatement = null;
        connection = NamedQueryConnectionFactory.getConnection(tenant);
        connection.setAutoCommit(true);
        preparedStatement = NamedPreparedStatement.prepareStatement(connection, queryToBeExecuted);
        List<String> paramList = preparedStatement.getlstParameters();
        List<String> missingParamList = new ArrayList<String>();
        if (CollectionUtils.isNotEmpty(paramList)) {
            if (CollectionUtils.isEmpty(variables)) {
                throw new NamedQueryRuntimeException("variable map is empty. List of missing parameter list: " + paramList);
            } else {
                for (String param : paramList) {
                    NamedQueryVariable nqv = new NamedQueryVariable();
                    nqv.setName(param);
                    boolean variableExist = variables.contains(nqv);
                    if (!variableExist) {
                        missingParamList.add(param);
                    }
                }
                if (CollectionUtils.isNotEmpty(missingParamList)) {
                    throw new NamedQueryRuntimeException("List of missing parameter list: " + missingParamList);
                } else {
                    for (NamedQueryVariable variable : variables) {
                        if (null != variable.getType()) {
                            switch (variable.getType().toLowerCase()) {
                                case "string":
                                    log.info(String.format("Got type, %s", variable.getType()));
                                    preparedStatement.setString(variable.getName(), variable.getValue());
                                    break;
                                case "number":
                                    log.info(String.format("Got type, %s", variable.getType()));
                                case "integer":
                                    log.info(String.format("Got type, %s", variable.getType()));
                                    preparedStatement.setInt(variable.getName(), Integer.valueOf(variable.getValue()));
                                    break;
                                case "boolean":
                                    log.info(String.format("Got type, %s", variable.getType()));
                                    preparedStatement.setBoolean(variable.getName(), Boolean.valueOf(variable.getValue()));
                                    break;
                                case "date":
                                    log.info(String.format("Got type, %s", variable.getType()));
                                    preparedStatement.setDate(variable.getName(), Date.valueOf(variable.getValue()));
                                    break;
                                case "timestamp":
                                    log.info(String.format("Got type, %s", variable.getType()));
                                    preparedStatement.setTimestamp(variable.getName(), Timestamp.valueOf(variable.getValue()));
                                    break;
                                default:
                                    log.info(String.format("Got unsupported type %s , Using default type", variable.getType()));
                                    preparedStatement.setString(variable.getName(), variable.getValue());
                                    break;
                            }
                        } else {
                            preparedStatement.setString(variable.getName(), variable.getValue());
                        }
                    }
                }
            }
        }

        returnMap.put("connection", connection);
        returnMap.put("preparedStatement", preparedStatement);
        return returnMap;
    }

    @Override
    public String previewQuery(String tenant, String datamodel, NamedQueryRequest request) {
        String queryName = request.getQuery();
        log.info("Previewquery "+queryName);
        List<NamedQueryVariable> variables = request.getVariables();

        String query = getQuery(queryName);
        Connection connection = null;
        NamedPreparedStatement preparedStatement = null;
        try {
            Map<String, Object> previewMap = preview(tenant, datamodel, query, variables);
            connection = (Connection) previewMap.get("connection");
            preparedStatement = (NamedPreparedStatement) previewMap.get("preparedStatement");
        } catch (SQLException e) {
            log.error(String.format("Exception occurred while executing %s.", queryName), e);
            throw new NamedQueryRuntimeException(String.format("Exception occurred while executing %s.", queryName));
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.getPreparedStatement().close();
                } catch (SQLException e) {
                    log.error("Error closing prepared statement.", e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.error("Error closing connection.", e);
                }
            }
        }
        return preparedStatement.getQuery();
    }

    @Override
    public String getQuery(String queryName) {
        if(!queryMap.containsKey(queryName)){
            SqlResource query = AppStateService.getSqlResource(queryName);
            if(null != query) {
                queryMap.put(queryName, query);
            } else {
                throw new NamedQueryRuntimeException("No Query found with name : "+queryName);
            }

        }
        return queryMap.get(queryName).getFileContent();
    }

    @Override
    public NamedQueryResponse getJobIdStatus(String jobId) {
        log.info(String.format("Responding status for %s", jobId));
        return namedQueryResponseMap.get(jobId);
    }

    @Override
    public void setJobIdStatus(NamedQueryResponse response) {
        namedQueryResponseMap.put(response.getJobId(), response);
    }

    @Override
    public void updateMap(SqlResource resource){
        this.queryMap.put(resource.getFileName(),resource);
    }

}
