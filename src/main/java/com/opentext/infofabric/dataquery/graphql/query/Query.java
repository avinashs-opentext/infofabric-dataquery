/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.query;

import com.healthmarketscience.sqlbuilder.OrderObject;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.RootDataFetcher;
import graphql.language.Argument;
import graphql.language.EnumValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.schema.GraphQLObjectType;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Query {

    private static final long DEFAULT_LIMIT = 100;
    private static final long MAX_LIMIT = 1000;

    private static final String SKIP_KEY = "skip";
    private static final String LIMIT_KEY = "limit";
    private static final String SORT_KEY = "sort";
    private static final String FIELD_NAME_KEY = "field";
    private static final String DIR_NAME_KEY = "direction";
    private static final String DIR_ASC = "ASC";
    private static final String AGGREGATE_NAME_KEY = "aggregate";

    private final Collection collection;
    private final ProjectionSet projectionSet;
    private FilterSet filterSet;
    private final Long skip;
    private final Long limit;
    private List<JoinCondition> joinConditions = new ArrayList<>();
    private Sort sort;
    private Aggregation aggregation;

    public Query(Collection collection, ProjectionSet projectionSet, FilterSet filterSet, Long skip, Long limit, Sort sort, Aggregation aggregation) {
        this.collection = collection;
        this.projectionSet = projectionSet;
        this.filterSet = filterSet;
        this.skip = skip;
        this.limit = limit;
        this.sort = sort;
        this.aggregation = aggregation;
    }

    // Parses a single query object
    public static Query build(GraphQLObjectType currentType, SelectionSet selections, List<Argument> arguments, QueryContext queryContext) {
        ProjectionSet projectionSet = ProjectionSet.buildAttributeProjection(queryContext.getQueryModel(), currentType, selections);
        String idField = projectionSet.getIdField();
        Long skip = 0L;
        Long limit = DEFAULT_LIMIT;
        String aggregate = null;

        FilterSet filterSet = null;
        String sortField = null;
        OrderObject.Dir direction = null;

        validateAggregateResult(queryContext, projectionSet);

        if (queryContext.getQueryType().equals(RootDataFetcher.QueryType.BY_ID)) {
            filterSet = FilterSet.buildIdFilter(idField, arguments.get(0).getValue());
        } else if (queryContext.getQueryType().equals(RootDataFetcher.QueryType.QUERY) ||
                queryContext.getQueryType().equals(RootDataFetcher.QueryType.AGGREGATE)) {
            filterSet = FilterSet.buildQueryFilter(arguments, currentType);
        }
        // Parse skip and limit from arguments
        for (Argument argument : arguments) {
            if (argument.getName().equals(SKIP_KEY)) {
                skip = ((IntValue) argument.getValue()).getValue().longValue();
            } else if (argument.getName().equals(LIMIT_KEY)) {
                limit = ((IntValue) argument.getValue()).getValue().longValue();
            } else if (argument.getName().equals(SORT_KEY)) {
                List<ObjectField> sortFields = ((ObjectValue) argument.getValue()).getObjectFields();
                for (ObjectField of : sortFields) {
                    if (of.getName().equals(FIELD_NAME_KEY)) {
                        sortField = ((StringValue) of.getValue()).getValue();
                        if (currentType.getFieldDefinition(sortField) == null) {
                            throw new DataqueryRuntimeException(String.format("User submitted sort field with name { %s} does not exist in SDL/datamodel", sortField));
                        }
                    } else if (of.getName().equals(DIR_NAME_KEY)) {
                        direction = DIR_ASC.equals(((EnumValue) of.getValue()).getName()) ?
                                OrderObject.Dir.ASCENDING : OrderObject.Dir.DESCENDING;
                    }
                }
            } else if (argument.getName().equals(AGGREGATE_NAME_KEY)) {
                aggregate = ((EnumValue) argument.getValue()).getName();
                limit = null;
                skip = null;
            }
        }

        if (limit != null && limit > MAX_LIMIT) {
            throw new DataqueryRuntimeException(String.format("Limit %s is more than the allowed maximum %s.", limit, MAX_LIMIT));
        }
        Sort sort = new Sort(sortField == null ? idField : sortField, direction == null ? OrderObject.Dir.ASCENDING : direction);

        return new Query(new Collection(currentType, queryContext), projectionSet, filterSet, skip, limit, sort, new Aggregation(aggregate));
    }

    private static void validateAggregateResult(QueryContext queryContext, ProjectionSet projectionSet) {
        boolean containsAggregateResult = isContainsAggregateResult(projectionSet);
        if (!(queryContext.getQueryType().equals(RootDataFetcher.QueryType.AGGREGATE))) {
            if (containsAggregateResult) {
                throw new DataqueryRuntimeException(String.format("dq_aggregation_result is selected as one of the projections for non Aggregate method { %s }", queryContext.getQueryType()));
            }
        } else {
            if (!containsAggregateResult) {
                throw new DataqueryRuntimeException("Aggregate method is called without selecting dq_aggregation_result");
            }
        }
    }

    private static boolean isContainsAggregateResult(ProjectionSet projectionSet) {
        boolean containsAggregateResult = false;
        for (Projection projection : projectionSet.getProjections()) {
            if (DataqueryConstants.DQ_AGGREGATION_RESULT.equals(projection.getField())) {
                containsAggregateResult = true;
                break;
            }
        }
        return containsAggregateResult;
    }

    public Collection getCollection() {
        return collection;
    }

    public ProjectionSet getProjectionSet() {
        return projectionSet;
    }

    public FilterSet getFilterSet() {
        return filterSet;
    }

    public void setFilterSet(FilterSet filterSet) {
        this.filterSet = filterSet;
    }

    public Long getSkip() {
        return skip;
    }

    public Long getLimit() {
        return limit;
    }

    public JoinCondition getFirstJoinCondition() {
        if (CollectionUtils.isEmpty(joinConditions)) {
            return null;
        }
        return joinConditions.get(0);
    }

    public void addJoinCondition(JoinCondition joinCondition) {
        this.joinConditions.add(joinCondition);
    }

    public Sort getSort() {
        return sort;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    @Override
    public String toString() {
        return String.format("{ Collection = %s, Projection = %s, Filters = %s, Aggregation = %s }",
                collection.getName(),
                projectionSet.toString(),
                (filterSet != null ? filterSet.toString() : "()"),
                (aggregation != null ? aggregation : "()"));

    }

    @Override
    public int hashCode() {
        return Objects.hash(collection, projectionSet, filterSet, joinConditions, skip, limit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Query query = (Query) o;
        return Objects.equals(collection, query.collection) &&
                Objects.equals(projectionSet, query.projectionSet) &&
                Objects.equals(filterSet, query.filterSet);
    }

    public List<JoinCondition> getJoinConditions() {
        return joinConditions;
    }
}
