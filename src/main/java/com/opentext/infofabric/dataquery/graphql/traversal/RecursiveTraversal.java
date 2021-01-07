/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.traversal;

import com.healthmarketscience.sqlbuilder.OrderObject;
import com.opentext.infofabric.dataquery.exception.DataqueryException;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.RootDataAccess;
import com.opentext.infofabric.dataquery.graphql.dataloaders.DataQueryLoader;
import com.opentext.infofabric.dataquery.graphql.dataloaders.QueryLoaderFactory;
import com.opentext.infofabric.dataquery.graphql.helpers.TypeMapper;
import com.opentext.infofabric.dataquery.graphql.query.Collection;
import com.opentext.infofabric.dataquery.graphql.query.FilterSet;
import com.opentext.infofabric.dataquery.graphql.query.JoinCondition;
import com.opentext.infofabric.dataquery.graphql.query.ProjectionSet;
import com.opentext.infofabric.dataquery.graphql.query.Query;
import com.opentext.infofabric.dataquery.graphql.query.QueryContext;
import com.opentext.infofabric.dataquery.graphql.query.Sort;
import com.opentext.infofabric.dataquery.graphql.results.ResultList;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import io.prometheus.client.Gauge;
import org.dataloader.DataLoaderOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.opentext.infofabric.dataquery.DataqueryConstants.PROMETHEUS_METRICS_ROOT;
import static com.opentext.infofabric.dataquery.DataqueryConstants.READ_PRIVILEGES;

public class RecursiveTraversal implements DataQueryTraversal {

    private static final Logger logger = LoggerFactory.getLogger(RecursiveTraversal.class);
    private static final DataLoaderOptions DEFAULT_LOADER_OPTIONS = new DataLoaderOptions().setCachingEnabled(true).setBatchingEnabled(true).setMaxBatchSize(100);
    private static final Gauge cacheHitAndMissRatio = Gauge.build()
            .name(PROMETHEUS_METRICS_ROOT + "dataloader_cache_hit_and_miss_ratio")
            .help("DataLoader Cache Hit and Miss Ratio")
            .register();

    @Override
    public ResultList execute(QueryContext queryContext, GraphQLType rootType, Field rootField, SelectionSet rootSelections, List<Argument> rootArguments) {
        DataQueryLoader dataLoader = QueryLoaderFactory.getBatchLoader(queryContext.getViewType(), DEFAULT_LOADER_OPTIONS);
        cacheHitAndMissRatio.set(dataLoader.getStatistics().getCacheHitRatio());

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Executing method %s using recursiveQueryTraversal type %s and loader type %s", rootField.getName(), this.getClass().getCanonicalName(), dataLoader.getClass().getCanonicalName()));
        }

        //Object security
        validateSecurity(queryContext, (GraphQLObjectType) rootType);

        return traverseAndLoad(queryContext, (GraphQLObjectType) rootType, rootSelections, rootArguments, dataLoader);
    }

    public ResultList traverseAndLoad(QueryContext queryContext, GraphQLObjectType rootType, SelectionSet rootSelections, List<Argument> rootArguments, DataQueryLoader dataLoader) {
        Query type = Query.build(rootType, rootSelections, rootArguments, queryContext);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Built root query: %s", type));
        }

        AtomicReference<Throwable> innerException = new AtomicReference<>();
        try {
            // Load root type set
            dataLoader.load(type).exceptionally(exception -> {
                innerException.set(exception);
                return null;
            });

            List<ResultList> resultLists = dataLoader.dispatchAndJoin();

            if (innerException.get() instanceof Exception) {
                throw (Exception) innerException.get();
            }

            if (resultLists == null || resultLists.size() == 0) {
                throw new DataqueryException("No result set returned.");
            }

            // Only 1 resultset for root query
            ResultList rootResults = resultLists.get(0);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Resolving children for root set: %s", rootResults));
            }

            // Resolve all nested types, rootResults gets populated
            resolve(queryContext, rootType, rootSelections, dataLoader, rootResults, innerException);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Root results resolved to: %s", rootResults));
            }

            if (innerException.get() instanceof Exception) {
                throw (Exception) innerException.get();
            }

            return rootResults;
        } catch (Exception e) {
            throw new DataqueryRuntimeException(e);
        }
    }

    private void resolve(QueryContext queryContext, GraphQLObjectType parentType, SelectionSet parentSelectionSet,
                         DataQueryLoader dataLoader, ResultList resultList, AtomicReference<Throwable> innerException) {
        if (resultList == null || resultList.size() == 0) {
            return;
        }
        parentSelectionSet.getSelections().forEach(selection -> {
            // Selections are always(tm) fields
            if (!(selection instanceof Field)) {
                throw new DataqueryRuntimeException(String.format("Unknown selection type %s. Only Field supported.", selection.getClass()));
            }

            Field selectedField = (Field) selection;

            // Get field as defined in the SDL
            GraphQLFieldDefinition fieldSDL = parentType.getFieldDefinition(selectedField.getName());
            // Get the type of the selected field
            GraphQLObjectType joinType = getObjectType(fieldSDL);
            if (joinType == null) {
                // Not an object type, can't traverse. Phew.
                return;
            }

            //Object security
            validateSecurity(queryContext, joinType);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Resolving field %s.%s", parentType.getName(), selectedField.getName()));
            }

            traverseJoin(dataLoader, resultList, selectedField, fieldSDL, joinType, queryContext, innerException);

            try {
                // Dispatch all queries. This will trigger thenAccept (or exceptionally) handler defined above.
                dataLoader.dispatchAndJoin();
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Dataloader dispatch called, statistics %s ", dataLoader.getStatistics()));
                }
            } catch (Exception e) {
                throw new DataqueryRuntimeException(e);
            }
        });
    }

    // Selections contain a relationship, traverse the join and load data
    private void traverseJoin(DataQueryLoader dataLoader, ResultList resultList, Field selectedField,
                              GraphQLFieldDefinition fieldSDL, GraphQLObjectType joinType, QueryContext queryContext,
                              AtomicReference<Throwable> innerException) {
        // Collection will be same for every query
        Collection collection = new Collection(joinType, queryContext);

        // Projection set will be same for every query
        ProjectionSet projections = ProjectionSet.buildAttributeProjection(RootDataAccess.QueryModel.MULTI_QUERY, joinType, selectedField.getSelectionSet());

        // Join filters will be the same for every query
        List<Argument> arguments = selectedField.getArguments();
        TypeMapper.bindVariables(arguments, queryContext.getVariables(), fieldSDL);
        FilterSet filterSet = FilterSet.buildQueryFilter(arguments, joinType);
        // Join conditions will be different depending on results from previous query.
        for (Map<String, Object> parentResult : resultList) {
            // Use field directive to build join info
            JoinCondition joinInfo = JoinCondition.build(RootDataAccess.QueryModel.MULTI_QUERY, fieldSDL, parentResult);
            if (joinInfo == null) {
                continue;
            }
            String parentIdFieldOnChildType = null;
            if (joinType.getFieldDefinition(joinInfo.getParentIdFieldOnChild()) != null) {
                GraphQLType type = joinType.getFieldDefinition(joinInfo.getParentIdFieldOnChild()).getType();
                parentIdFieldOnChildType = TypeMapper.getNonNullType(type).getName();
            }
            projections.addProjection(joinInfo.getParentIdFieldOnChild(), null, parentIdFieldOnChildType);

            Sort sort = new Sort(projections.getIdField(), OrderObject.Dir.ASCENDING);

            // Query with unique join conditions. Create a separate query instance to make caching by query possible.
            Query query = new Query(collection, projections, filterSet, null, null, sort, null);
            query.addJoinCondition(joinInfo);
            loadAndAssign(dataLoader, query, selectedField, parentResult, queryContext, joinType, innerException);
        }
    }

    // Call data loader and assign result map to parent map. Call resolve method again for result map.
    private void loadAndAssign(DataQueryLoader dataLoader, Query query, Field selectedField,
                               Map<String, Object> parentResult, QueryContext queryContext,
                               GraphQLObjectType joinType, AtomicReference<Throwable> innerException) {
        // Add query to be loaded, set up success handler that populates the selected field with the results.
        // ThenAccept function will be triggered AFTER dataLoader.dispatchAndJoin() is called.
        dataLoader.load(query).thenAccept(childResult -> {
            if (childResult == null) {
                throw new DataqueryRuntimeException("Null resultList returned. Data loader should return an empty list in case of no results. " +
                        "* to 1 relationships will be flattened by the data fetcher.");
            }
            // Append result list to the parent object
            if (query.getFirstJoinCondition().hasMany()) {
                // Expecting multiple results - add all
                parentResult.put(selectedField.getName(), childResult);
            } else { // Expecting a single result object, flatten array to single map
                if (childResult.size() > 1) {
                    throw new DataqueryRuntimeException("Expected a single result object (one-to-one or many-to-one schema) but multiple result objects were returned. Check the schema.");
                }
                // Add a single child object to parent object if exists, or null if it doesn't.
                if (childResult.size() > 0) {
                    parentResult.put(selectedField.getName(), childResult.get(0));
                } else {
                    parentResult.put(selectedField.getName(), null);
                }
            }

            // Resolve child result's nested types so that results get recursively added to the parent object
            resolve(queryContext, joinType, selectedField.getSelectionSet(), dataLoader, childResult, innerException);
        }).exceptionally(exception -> {
            logger.error(String.format("Exception occured when dispatching queries to data loader %s", dataLoader.getClass().getCanonicalName()), exception);
            innerException.set(exception);
            return null;
        });
    }

    private GraphQLObjectType getObjectType(GraphQLFieldDefinition fieldSDL) {
        GraphQLType wrappedType = TypeMapper.getWrappedType(fieldSDL.getType());
        if (wrappedType instanceof GraphQLObjectType) {
            return (GraphQLObjectType) wrappedType;
        }
        return null;
    }

    /**
     * Validate the object security for the given types in the multi query case
     *
     * @param queryContext - query context containing the access privileges
     * @param type         - Graph QL object type
     */
    private void validateSecurity(QueryContext queryContext, GraphQLObjectType type) {
        if (!queryContext.getAccessPrivileges().isAuthorized(type.getName(), READ_PRIVILEGES)) {
            throw new DataqueryRuntimeException("Not authorized to query type - " + type.getName());
        }
    }
}
