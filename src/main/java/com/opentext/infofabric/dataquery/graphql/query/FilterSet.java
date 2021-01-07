/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.query;

import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.helpers.TypeMapper;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class FilterSet {

    private static final String LOGICAL_OPERATOR_KEY = "op";
    private static final String COMPARISON_OPERATOR_KEY = "op";
    private static final String FILTERS_KEY = "filters";
    private static final String FILTERSETS_KEY = "filtersets";
    private static final String ROOT_QUERY_KEY = "filterset";
    public static final String FIELD_NAME_KEY = "field";
    private static final String NEGATION_KEY = "not";
    private static final String VALUE_KEY = "value";
    private static final String ID_TYPE = "ID";


    public boolean isNegation() {
        return negation;
    }

    public Filter getIdFilter() {
        if (filters != null) {
            for (Filter filter : filters) {
                if (filter.isID()) {
                    return filter;
                }
            }
        }
        return null;
    }

    public void addFilter(Filter filter) {
        if (filters == null) {
            filters = new ArrayList<>();
        }
        filters.add(filter);
    }

    public enum LogicalOperator {
        AND, OR
    }

    public enum ComparisonOperator {
        EQ, NE, LT, LE, GT, GE, LIKE, IN
    }


    private List<Filter> filters;
    private List<FilterSet> filtersets;
    private LogicalOperator logicalOperator;
    private boolean negation;


    static FilterSet buildIdFilter(String name, Value argument) {
        if (argument == null) {
            return null;
        }
        FilterSet filterSet = new FilterSet();
        filterSet.filters = new ArrayList<>();

        Object id = TypeMapper.toJavaType(argument);

        Filter filter = new Filter(name, ID_TYPE, (id instanceof Collection) ? ComparisonOperator.IN : ComparisonOperator.EQ, id, false);
        filter.isID(true);
        filterSet.filters.add(filter);
        return filterSet;
    }

    public static FilterSet buildQueryFilter(List<Argument> arguments, GraphQLObjectType currentType) {
        if (arguments == null) {
            return null;
        }
        for (Argument argument : arguments) {
            if (argument.getName().equals(ROOT_QUERY_KEY)) {
                return parseQueryTree(argument, currentType);
            }
        }
        return null;
    }

    private static FilterSet parseQueryTree(Argument queryArgument, GraphQLObjectType currentType) {
        ObjectValue filterSetObj = (ObjectValue) queryArgument.getValue();
        return parseQueryFilter(filterSetObj, currentType);
    }

    private static FilterSet parseQueryFilter(ObjectValue filterSetObj, GraphQLObjectType currentType) {
        FilterSet filterSet = new FilterSet();
        for (ObjectField objectField : filterSetObj.getObjectFields()) {
            parseQueryFilterProperty(objectField, filterSet, currentType);
        }
        validateQueryFilter(filterSet);
        return filterSet;
    }

    private static void validateQueryFilter(FilterSet filterSet) {
        if (filterSet.filters != null && filterSet.filtersets != null) {
            throw new DataqueryRuntimeException("Query object can only contain either filter array or nested filter sets, not both.");
        } else if (filterSet.filters == null && filterSet.filtersets == null) {
            throw new DataqueryRuntimeException("Query object has to contain either filters or nested filter sets.");
        }

        if ((filterSet.filters != null && filterSet.filters.size() > 1) || (filterSet.filtersets != null && filterSet.filtersets.size() > 1)) {
            if (filterSet.logicalOperator == null) {
                throw new DataqueryRuntimeException("Logical operator missing when multiple filters provided.");
            }
        } else {
            // Only a single filter, nothing to apply a logical operator to
            filterSet.logicalOperator = null;
        }
    }

    private static void parseQueryFilterProperty(ObjectField objectField, FilterSet filterSet, GraphQLObjectType currentType) {
        if (objectField.getName().equals(FILTERS_KEY)) {
            ArrayValue filterList = (ArrayValue) objectField.getValue();
            filterSet.filters = parseFilters(filterList, currentType);
        } else if (objectField.getName().equals(FILTERSETS_KEY)) {
            ArrayValue queryList = (ArrayValue) objectField.getValue();
            if (filterSet.filtersets == null) {
                filterSet.filtersets = new ArrayList<>();
            }
            for (Value q : queryList.getValues()) {
                filterSet.filtersets.add(parseQueryFilter((ObjectValue) q, currentType));
            }
        } else if (objectField.getName().equals(LOGICAL_OPERATOR_KEY)) {
            EnumValue operatorValue = (EnumValue) objectField.getValue();
            filterSet.logicalOperator = LogicalOperator.valueOf(operatorValue.getName());
        } else if (objectField.getName().equals(NEGATION_KEY)) {
            BooleanValue operatorValue = (BooleanValue) objectField.getValue();
            filterSet.negation = operatorValue.isValue();
        }
    }

    private static List<Filter> parseFilters(ArrayValue filterList, GraphQLObjectType currentType) {
        List<Filter> filters = new ArrayList<>();
        for (Value f : filterList.getValues()) {
            ObjectValue filterObj = (ObjectValue) f;
            String fieldName = null;
            String fieldType = null;
            ComparisonOperator comparisonOperator = null;
            Object value = null;
            boolean isNot = false;
            List<String> path = new ArrayList<>();
            path.add(currentType.getName());
            for (ObjectField objectField : filterObj.getObjectFields()) {
                if (objectField.getName().equals(FIELD_NAME_KEY)) {
                    fieldName = ((StringValue) objectField.getValue()).getValue();
                    GraphQLObjectType tempType = currentType;

                    if (fieldName.contains(".")) {
                        String[] fieldPath = fieldName.split("\\.");
                        fieldName = fieldPath[fieldPath.length - 1];
                        for (int i = 0; i < fieldPath.length - 1; ++i) {
                            // Fail if there's no relationship with the path, or the field is not an object type
                            GraphQLFieldDefinition tempFieldDef = tempType.getFieldDefinition(fieldPath[i]);
                            if (tempFieldDef == null || !(TypeMapper.getWrappedType(tempFieldDef.getType()) instanceof GraphQLObjectType)) {
                                throw new DataqueryRuntimeException(String.format("User submitted field name { %s } does not exist in SDL/datamodel", fieldName));
                            }
                            tempType = (GraphQLObjectType) TypeMapper.getWrappedType(tempFieldDef.getType());
                            path.add(fieldPath[i]);
                        }
                    }

                    if (tempType.getFieldDefinition(fieldName) == null) {
                        throw new DataqueryRuntimeException(String.format("User submitted field name { %s } does not exist in SDL/datamodel", fieldName));
                    }
                    fieldType = getFieldTypeOfGraphQLField(tempType, fieldName);
                    continue;
                }
                if (objectField.getName().equals(COMPARISON_OPERATOR_KEY)) {
                    comparisonOperator = ComparisonOperator.valueOf(((EnumValue) objectField.getValue()).getName());
                    continue;
                }
                if (objectField.getName().equals(VALUE_KEY)) {
                    value = TypeMapper.toJavaType(objectField.getValue());
                    continue;
                }
                if (objectField.getName().equals(NEGATION_KEY)) {
                    isNot = ((BooleanValue) objectField.getValue()).isValue();
                }
            }
            filters.add(new Filter(fieldName, fieldType, comparisonOperator, value, isNot, path));
        }
        return filters;
    }

    public static String getFieldTypeOfGraphQLField(GraphQLObjectType currentType, String fieldName) {
        String fieldType;
        GraphQLType graphQLFieldType = TypeMapper.getNonNullType(currentType.getFieldDefinition(fieldName).getType());
        if (graphQLFieldType.getName().equals("ID")) {
            // Source DB will figure out what field type is when it is ID
            fieldType = ID_TYPE;
        } else {
            fieldType = graphQLFieldType.getName();
        }
        return fieldType;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public List<FilterSet> getFiltersets() {
        return filtersets;
    }

    public void initFiltersets() {
        this.filtersets = new ArrayList<>();
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    @Override
    public String toString() {
        if (filters != null) {
            return stringify(filters);
        }
        return stringify(filtersets);
    }

    private String stringify(List list) {
        String filtersStr;
        String[] filterStrArr = new String[list.size()];
        IntStream.range(0, list.size()).forEach(i -> {
            Object f = list.get(i);
            filterStrArr[i] = f.toString();
        });
        filtersStr = String.join(" " + logicalOperator + " ", filterStrArr);
        return (negation ? "NOT " : "") + "(" + filtersStr + ")";
    }
}