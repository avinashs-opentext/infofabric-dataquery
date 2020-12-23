/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.dataloaders.rdbms;

import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgLimitClause;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgOffsetClause;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.graphql.query.Projection;
import com.liaison.dataquery.graphql.query.ProjectionSet;
import com.liaison.dataquery.graphql.query.Query;
import com.liaison.dataquery.graphql.query.Sort;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SQLBatchParser extends SQLQueryParser {


    private SQLBatchParser() {
    }

    public static String parse(Query query) {
        List<Query> queries = new ArrayList<>();
        queries.add(query);
        return parse(queries);
    }


    public static String parse(List<Query> queries) {
        if (queries == null || queries.size() == 0) {
            return null;
        }
        Query firstQuery = queries.get(0);
        String aggregate = (firstQuery.getAggregation() != null ? firstQuery.getAggregation().getType() : null);
        DbSpec dbSpec = new DbSpec();

        // Data model name == Schema name
        DbSchema dbSchema = new DbSchema(dbSpec, doubleQuote(firstQuery.getCollection().getDatamodel()));
        DbTable table = new DbTable(dbSchema, doubleQuote(firstQuery.getCollection().getName()));

        // SELECT DISTINCT
        SelectQuery selectQuery = new SelectQuery(true);
        if (firstQuery.getLimit() != null) {
            selectQuery.addCustomization(new PgLimitClause(firstQuery.getLimit()));
        }
        if (firstQuery.getSkip() != null) {
            selectQuery.addCustomization(new PgOffsetClause(firstQuery.getSkip()));
        }

        ComboCondition combinedQueryFilters = null;
        if (firstQuery.getFirstJoinCondition() != null && !StringUtils.isBlank(firstQuery.getFirstJoinCondition().getBridgeName())) {
            combinedQueryFilters = createSelectQueryForManyToManyJoins(queries, firstQuery, dbSchema, table, selectQuery);
        } else {
            if (aggregate == null) {
                setDefaultSorting(firstQuery.getSort());
                selectQuery.addOrdering(new DbColumn(table, firstQuery.getSort().getQuotedField(), null),
                        firstQuery.getSort().getDirection());
                setDefaultProjections(firstQuery.getProjectionSet());
                // Projections will be same for every query
                firstQuery.getProjectionSet().getProjections().forEach(proj -> selectQuery.addColumns(new DbColumn(table, proj.getQuotedField(), proj.getDataType())));
            } else {
                updateSelectQueryForAggregate(firstQuery, aggregate, table, selectQuery);
            }
            combinedQueryFilters = addJoinCondition(queries, table);
        }

        // Filter sets will be same among all queries
        if (firstQuery.getFilterSet() != null) {
            Condition filterConditions = parseFilterSet(table, firstQuery.getFilterSet());
            if (combinedQueryFilters != null) {
                combinedQueryFilters.addCondition(filterConditions);
            } else {
                selectQuery.addCondition(filterConditions);
            }
        }

        if (combinedQueryFilters != null) {
            selectQuery.addCondition(combinedQueryFilters);
        }

        return selectQuery.validate().toString();
    }

    private static ComboCondition createSelectQueryForManyToManyJoins(List<Query> queries, Query firstQuery, DbSchema dbSchema, DbTable table, SelectQuery selectQuery) {
        DbTable bridgeTable = new DbTable(dbSchema, doubleQuote(firstQuery.getFirstJoinCondition().getBridgeName()));
        DbColumn bridgeColumn = new DbColumn(bridgeTable, doubleQuote(firstQuery.getFirstJoinCondition().getChildBridgeKeyFieldName()), null);
        DbColumn parentColumn = new DbColumn(table, doubleQuote(firstQuery.getFirstJoinCondition().getParentIdFieldOnChild()), null);
        setDefaultSorting(firstQuery.getSort());
        selectQuery.addOrdering(new DbColumn(table, firstQuery.getSort().getQuotedField(), null),
                firstQuery.getSort().getDirection());
        selectQuery.addJoin(SelectQuery.JoinType.INNER, bridgeTable, table, bridgeColumn, parentColumn);
        firstQuery.getProjectionSet().getProjections().forEach(proj -> selectQuery.addColumns(new DbColumn(table, proj.getQuotedField(), proj.getDataType())));
        selectQuery.addColumns(new DbColumn(bridgeTable, doubleQuote(firstQuery.getFirstJoinCondition().getParentBridgeKeyFieldName()), null));
        return addJoinCondition(queries, bridgeTable);
    }

    private static void updateSelectQueryForAggregate(Query firstQuery, String aggregate, DbTable table, SelectQuery selectQuery) {
        Set<Projection> aggregateProjectionSet = addProjectionsForAggregate(firstQuery.getProjectionSet().getProjections());
        String primaryKeyField = getPrimaryKeyFieldName(firstQuery.getProjectionSet().getProjections());
        if ((firstQuery.getSort().getField() != null) &&
                !(primaryKeyField != null && primaryKeyField.equals(firstQuery.getSort().getField()))) {
            selectQuery.addOrdering(new DbColumn(table, firstQuery.getSort().getQuotedField(), null),
                    firstQuery.getSort().getDirection());
        }
        selectQuery.addAliasedColumn(new CustomSql(aggregate + "(" + singleQuote(primaryKeyField) + ")"), DataqueryConstants.DQ_AGGREGATION_RESULT);
        if (aggregateProjectionSet.isEmpty()) {
            selectQuery.addFromTable(table);
        } else {
            // Projections will be same for every query
            aggregateProjectionSet.forEach(proj -> selectQuery.addColumns(new DbColumn(table, proj.getQuotedField(), proj.getDataType())));
            aggregateProjectionSet.forEach(proj -> selectQuery.addGroupings(new DbColumn(table, proj.getQuotedField(), proj.getDataType())));
        }
    }

    private static void setDefaultProjections(ProjectionSet projectionSet) {
        boolean hasId = false;
        for (Projection projection : projectionSet.getProjections()) {
            if ("ID".equals(projection.getDataType())) {
                hasId = true;
            }
        }
        if (!hasId) {
            projectionSet.addProjection(DEFAULT_ID, null, "Long");
        }

    }

    private static Set<Projection> addProjectionsForAggregate(Set<Projection> projectionSet) {
        Set<Projection> aggregateProjectionSet = new HashSet<>();
        for (Projection projection : projectionSet) {
            if (!("ID".equals(projection.getDataType()) ||
                    projection.getField().equals(DataqueryConstants.DQ_AGGREGATION_RESULT))) {
                aggregateProjectionSet.add(projection);
            }
        }
        return aggregateProjectionSet;
    }

    private static String getPrimaryKeyFieldName(Set<Projection> projectionSet) {
        for (Projection projection : projectionSet) {
            if ("ID".equals(projection.getDataType())) {
                return projection.getField();
            }
        }
        return DEFAULT_ID;
    }

    private static void setDefaultSorting(Sort sort) {
        if (sort.getField() == null) {
            sort.setField(DEFAULT_ID);
        }
    }

    private static ComboCondition addJoinCondition(List<Query> queries, DbTable table) {
        // If query has join condition AND with filters
        ComboCondition combinedQueryFilters = null;
        if (queries.stream().anyMatch(query -> query.getFirstJoinCondition() != null)) {
            combinedQueryFilters = new ComboCondition(ComboCondition.Op.AND);
            List<Object> parentIds = new ArrayList<>();
            String idFieldOnChild = null;
            for (Query query : queries) {
                if (query.getFirstJoinCondition() != null) {
                    if (idFieldOnChild == null) {
                        if (!StringUtils.isBlank(query.getFirstJoinCondition().getBridgeName())) {
                            idFieldOnChild = query.getFirstJoinCondition().getParentBridgeKeyFieldName();
                        } else {
                            idFieldOnChild = query.getFirstJoinCondition().getParentIdFieldOnChild();
                        }
                    }
                    Object parentTypeId = query.getFirstJoinCondition().getParentTypeId();
                    parentIds.add(parentTypeId);
                }
            }
            InCondition inCondition = new InCondition(new DbColumn(table, doubleQuote(idFieldOnChild), null), parentIds);
            combinedQueryFilters.addCondition(inCondition);
        }
        return combinedQueryFilters;
    }


}

