/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms;

import com.liaison.datagate.rdbms.sql.MVPreparedStatement;
import com.opentext.infofabric.dataquery.exception.DataLoaderException;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.dataloaders.DataQueryBatchLoader;
import com.opentext.infofabric.dataquery.graphql.query.Filter;
import com.opentext.infofabric.dataquery.graphql.query.FilterSet;
import com.opentext.infofabric.dataquery.graphql.query.Query;
import com.opentext.infofabric.dataquery.graphql.results.ResultList;
import com.opentext.infofabric.dataquery.graphql.results.ResultObject;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import io.prometheus.client.Histogram;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RDBMSBatchLoader extends RDBMSLoader implements DataQueryBatchLoader {

    private final static Logger logger = LoggerFactory.getLogger(RDBMSBatchLoader.class);
    // metrics to log the latency
    static final Histogram executeSqlMethodLatency = Histogram.build()
            .name(DataqueryConstants.PROMETHEUS_METRICS_ROOT + "rdbmsbatchloader_executeSql_latency_seconds")
            .help("RDBMSBatchLoader executeSql method latency in seconds.")
            .register();
    // metrics to get execute query latency
    static final Histogram executeQueryLatency = Histogram.build()
            .name(DataqueryConstants.PROMETHEUS_METRICS_ROOT + "rdbmsbatchloader_execute_query_latency_seconds")
            .help("RDBMSBatchLoader prepare query latency in seconds.")
            .register();

    @Override
    public CompletionStage<List<ResultList>> load(List<Query> queries) {
        //Note: Load needs to return as many ResultSets as there are Query objects.
        return CompletableFuture.supplyAsync(() -> {
            Histogram.Timer methodExecutionTimer = executeSqlMethodLatency.startTimer();
            try {
                // Distinct SQL clause having all filters with OR condition
                String sql = SQLBatchParser.parse(queries);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("RDBMSBatchLoader generated sql: %s", sql));
                }
                return executeSql(queries, sql);
            } catch (Exception e) {
                logger.error("Error running sql query", e);
                throw new DataLoaderException(e);
            } finally {
                methodExecutionTimer.observeDuration();
            }
        });
    }

    @Override
    public void validate(List<Query> queryKeys) {
        SQLBatchParser.parse(queryKeys);
    }

    @Override
    public Object getNativeQuery(List<Query> queryKeys) {
        return SQLBatchParser.parse(queryKeys);
    }

    private List<ResultList> executeSql(List<Query> originalQueries, String sql) {
        if (originalQueries == null || originalQueries.size() == 0) {
            return new ArrayList<>();
        }

        String tenant = originalQueries.get(0).getCollection().getTenant();
        final RDBMSConnectionProvider connectionProvider = RDBMSConnectionProvider.forTenant(tenant);

        // metrics
        Histogram.Timer queryTimer = null;

        // combinedResultList contains results for all the queries.
        // Note: Size of resultsPerQuery needs to match the size of originalQueries.
        ResultList combinedResultList;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             MVPreparedStatement mvPreparedStatement = new MVPreparedStatement(preparedStatement)) {

            List<Filter> filterArrayList = new ArrayList<>();
            FilterSet filterSet = originalQueries.get(0).getFilterSet();
            populateFilterArrayList(filterArrayList, filterSet);
            setFilterValuesOnPreparedStmt(mvPreparedStatement, filterArrayList);

            queryTimer = executeQueryLatency.startTimer();

            ResultSet resultSet = mvPreparedStatement.executeQuery();
            combinedResultList = resultSetToList(resultSet);
        } catch (SQLException e) {
            throw new DataqueryRuntimeException(e);
        } finally {
            if (queryTimer != null) {
                queryTimer.observeDuration();
            }
        }

        // Note: Size of resultsPerQuery needs to match the size of originalQueries.
        return matchQueriesToResults(originalQueries, combinedResultList);
    }



    private List<ResultList> matchQueriesToResults(List<Query> originalQueries, ResultList combinedResultList) {
        // resultsPerQuery holds a ResultList for each Query.
        // Note: Size of resultsPerQuery needs to match the size of originalQueries.
        List<ResultList> resultsPerQuery = new ArrayList<>();

        // Only single input query -> all results are for that query.
        if (originalQueries.size() == 1) {
            resultsPerQuery.add(combinedResultList);
            return resultsPerQuery;
        }

        // Loop through input queries and match results with each query.
        for (Query query : originalQueries) {
            if (query.getFirstJoinCondition() != null) {
                // Join is done by matching the parents PK or FK to the field on the child.
                String parentIdFieldOnChild = query.getFirstJoinCondition().getParentIdFieldOnChild();
                Object parentId = query.getFirstJoinCondition().getParentTypeId();
                String parentBridgeKeyFieldName = query.getFirstJoinCondition().getParentBridgeKeyFieldName();
                ResultList singleQueryResult = new ResultList();
                for (ResultObject result : combinedResultList) {
                    if (!StringUtils.isBlank(query.getFirstJoinCondition().getBridgeName())) {
                        if (result.containsKey(parentBridgeKeyFieldName) && result.get(parentBridgeKeyFieldName).equals(parentId)) {
                            singleQueryResult.add(result);
                        }
                    } else {
                        if (result.containsKey(parentIdFieldOnChild) && result.get(parentIdFieldOnChild).equals(parentId)) {
                            singleQueryResult.add(result);
                        }
                    }
                }
                // If there was no match an empty ResultList is added.
                resultsPerQuery.add(singleQueryResult);
            } else {
                logger.warn("Query contains no JoinCondition when batch loading. Can't assign results.");
                resultsPerQuery.add(new ResultList());
            }
        }

        return resultsPerQuery;
    }

}
