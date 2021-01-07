/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.Subquery;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgLimitClause;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgOffsetClause;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.graphql.query.JoinCondition;
import com.opentext.infofabric.dataquery.graphql.query.ProjectionSet;
import com.opentext.infofabric.dataquery.graphql.query.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class SQLSimpleParser extends SQLQueryParser {

    private final SQLAliasCache aliasCache = new SQLAliasCache();
    private final SQLQuery sqlQuery = new SQLQuery();

    public SQLQuery parse(Query query) {

        String aggregate = (query.getAggregation() != null ? query.getAggregation().getType() : null);
        DbSpec dbSpec = new DbSpec();

        DbSchema dbSchema = new DbSchema(dbSpec, doubleQuote(query.getCollection().getDatamodel()));

        String rootIdField = StringUtils.isEmpty(query.getProjectionSet().getIdField()) ? DEFAULT_ID : query.getProjectionSet().getIdField();
        String fromName = query.getCollection().getName();
        String fromAlias = aliasCache.addAliasMapping(fromName, false);
        DbTable fromTable = new DbTable(dbSchema, doubleQuote(fromName), fromAlias);

        aliasCache.addTable(fromName, fromTable);
        aliasCache.addTablePathKey(fromName, rootIdField);
        SelectQuery selectQuery = new SelectQuery(true);

        if (query.getLimit() != null) {
            selectQuery.addCustomization(new PgLimitClause(query.getLimit()));
        }
        if (query.getSkip() != null) {
            selectQuery.addCustomization(new PgOffsetClause(query.getSkip()));
        }

        selectQuery.addFromTable(fromTable);

        if (StringUtils.isNotEmpty(aggregate)) {
            buildSelectQuery(query, rootIdField, dbSchema, fromTable, selectQuery, false, true);
            selectQuery.addAliasedColumn(new CustomSql(aggregate + "(" + singleQuote(aliasCache.getRootObjectAlias() + "." + rootIdField) + ")"), doubleQuote(aliasCache.getRootObjectAlias() + "." + DataqueryConstants.DQ_AGGREGATION_RESULT));
            sqlQuery.setSql(selectQuery.toString());
            sqlQuery.setAliasCache(aliasCache);
            sqlQuery.setCartesian(false);
        } else {
            buildSelectQuery(query, rootIdField, dbSchema, fromTable, selectQuery, true, false);
            Subquery subquery = new Subquery(selectQuery);
            SelectQuery outerSelect = new SelectQuery(true);
            outerSelect.addCustomFromTable(subquery + " " + aliasCache.getRootObjectAlias());

            buildSelectQuery(query, rootIdField, dbSchema, fromTable, outerSelect, false, false);
            sqlQuery.setCartesian(true);
            sqlQuery.setSql(outerSelect.toString());
            sqlQuery.setAliasCache(aliasCache);
        }
        return sqlQuery;
    }

    private void buildSelectQuery(Query query, String rootIdField,
                                  DbSchema dbSchema, DbTable fromTable,
                                  SelectQuery selectQuery, boolean isSub,
                                  boolean isAggregate) {
        if (isAggregate) {
            addSortForAggregate(query, rootIdField, fromTable, selectQuery);
        } else {
            addSortWithPK(query, rootIdField, fromTable, selectQuery);
        }

        for (JoinCondition join : query.getJoinConditions()) {
            addJoin(query, selectQuery, dbSchema, fromTable, join, sqlQuery, isSub, isAggregate);
        }

        Condition filterSet = parseFilterSet(fromTable, query.getFilterSet(), sqlQuery, aliasCache);
        if (filterSet != null) {
            selectQuery.addCondition(filterSet);
        }

        addProjections(query, fromTable, selectQuery, isSub, isAggregate);
    }

    private void addJoin(Query query, SelectQuery selectQuery,
                         DbSchema schema, DbTable joinTo, JoinCondition join,
                         SQLQuery sqlQuery, boolean isSub, boolean isAggregate) {
        DbColumn parentColumn = new DbColumn(joinTo, doubleQuote(join.getParentIdField()), null);
        if (StringUtils.isNotEmpty(join.getBridgeName())) {
            DbTable bridgeTable = new DbTable(schema, doubleQuote(join.getBridgeName()));
            DbColumn bridgeColumn = new DbColumn(bridgeTable, doubleQuote(join.getParentBridgeKeyFieldName()), null);
            selectQuery.addJoin(SelectQuery.JoinType.LEFT_OUTER, joinTo, bridgeTable, bridgeColumn, parentColumn);
            joinTo = bridgeTable;
            parentColumn = new DbColumn(bridgeTable, doubleQuote(join.getChildBridgeKeyFieldName()), null);
        }
        String tablePath = aliasCache.getTablePath(join);
        String shortAlias = aliasCache.addAliasMapping(tablePath, join.hasMany());
        String primaryKeyForPath = getPrimaryKeyForPath(query.getProjectionSet(), join.getPath());
        aliasCache.addTablePathKey(tablePath, primaryKeyForPath);
        DbTable joinFrom = new DbTable(schema, doubleQuote(join.getTargetType()), shortAlias);
        aliasCache.addTable(tablePath, joinFrom);

        DbColumn childColumn = new DbColumn(joinFrom, doubleQuote(join.getParentIdFieldOnChild()), null);
        Condition sqlJoinCondition = new BinaryCondition(BinaryCondition.Op.EQUAL_TO, parentColumn, childColumn);
        if (join.getFilterSet() != null) {
            Condition condition = parseFilterSet(joinFrom, join.getFilterSet(), sqlQuery, null);
            sqlJoinCondition = new ComboCondition(ComboCondition.Op.AND, sqlJoinCondition, condition);
        }
        selectQuery.addJoin(SelectQuery.JoinType.LEFT_OUTER, joinTo, joinFrom, sqlJoinCondition);

        // Sub queries and aggregations are not sorted by joined tables because they're not part of the projection
        if (!isSub && !isAggregate) {
            selectQuery.addOrdering(new DbColumn(joinFrom, doubleQuote(primaryKeyForPath), null), OrderObject.Dir.DESCENDING);
        }

        for (JoinCondition childJoin : join.getChildren()) {
            addJoin(query, selectQuery, schema, joinFrom, childJoin, sqlQuery, isSub, isAggregate);
        }

    }

    private String getPrimaryKeyForPath(ProjectionSet projectionSet, List<String> path) {
        if (CollectionUtils.isNotEmpty(projectionSet.getPath()) && projectionSet.getPath().equals(path)) {
            return StringUtils.isEmpty(projectionSet.getIdField()) ? DEFAULT_ID : projectionSet.getIdField();
        }
        if (CollectionUtils.isEmpty(projectionSet.getChildren())) {
            return null;
        }
        for (ProjectionSet child : projectionSet.getChildren()) {
            String pk = getPrimaryKeyForPath(child, path);
            if (pk != null) {
                return pk;
            }
        }
        return null;
    }

    private void addProjections(Query query, DbTable fromTable, SelectQuery selectQuery, boolean isSub, boolean isAggregate) {
        if (isSub) {
            // Sub queries select all the columns, because outer query may need to filter by columns not part of the projections
            selectQuery.addAllTableColumns(fromTable);
            return;
        }
        SortedMap<String, DbColumn> columnSortedMap = new TreeMap<>();
        ProjectionSet projectionSet = query.getProjectionSet();
        addProjection(projectionSet, columnSortedMap, isSub, isAggregate);
        columnSortedMap.forEach((k, v) -> {
            selectQuery.addAliasedColumn(v, k);
            if (isAggregate) {
                selectQuery.addGroupings(v);
            }
        });
    }

    private void addProjection(ProjectionSet projectionSet, SortedMap<String, DbColumn> columnSortedMap, boolean isSub, boolean isAggregate) {
        String tablePath = aliasCache.getAlias(projectionSet.getPath());
        projectionSet.getProjections().forEach(proj -> {
            if (proj.getField().equals(DataqueryConstants.DQ_AGGREGATION_RESULT) || (isAggregate && proj.isAutoProjection())) {
                return;
            }
            String columnAlias = proj.getQuotedField();
            if (!isSub) {
                columnAlias = String.format("\"%s.%s\"", aliasCache.getAlias(tablePath), proj.getField());
            }
            DbColumn dbColumn = new DbColumn(aliasCache.getTable(tablePath),
                    proj.getQuotedField(), proj.getDataType());
            columnSortedMap.put(columnAlias, dbColumn);
        });
        if (!isAggregate && StringUtils.isEmpty(projectionSet.getIdField())) {
            String columnAlias = doubleQuote(DEFAULT_ID);
            if (!isSub) {
                columnAlias = String.format("\"%s.%s\"", aliasCache.getAlias(tablePath), DEFAULT_ID);
            }
            DbColumn dbColumn = new DbColumn(aliasCache.getTable(tablePath),
                    doubleQuote(DEFAULT_ID), "INT");
            columnSortedMap.put(columnAlias, dbColumn);
        }
        if (isSub || CollectionUtils.isEmpty(projectionSet.getChildren())) {
            return;
        }
        projectionSet.getChildren().forEach(projSet -> {
            addProjection(projSet, columnSortedMap, isSub, isAggregate);
        });
    }

    private void addSortWithPK(Query query, String rootIdField, DbTable fromTable, SelectQuery selectQuery) {
        if (query.getSort().getField() == null) {
            query.getSort().setField(rootIdField);
        }

        selectQuery.addOrdering(new DbColumn(fromTable, query.getSort().getQuotedField(), null),
                query.getSort().getDirection());

        //Always add a secondary sort for root object PK, so all rows for a single object instance are grouped together
        if (!query.getSort().getField().equals(rootIdField)
                && !query.getSort().getField().equals(DEFAULT_ID)) {
            selectQuery.addOrdering(new DbColumn(fromTable, doubleQuote(rootIdField), null),
                    OrderObject.Dir.ASCENDING);
        }
    }

    private void addSortForAggregate(Query query, String rootIdField, DbTable fromTable, SelectQuery selectQuery) {
        if (query.getSort().getField() == null || query.getSort().getField().equals(rootIdField)) {
            return;
        }
        selectQuery.addOrdering(new DbColumn(fromTable, query.getSort().getQuotedField(), null),
                query.getSort().getDirection());
    }

}
