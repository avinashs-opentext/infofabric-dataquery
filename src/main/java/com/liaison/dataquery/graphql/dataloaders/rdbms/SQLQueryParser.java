/**
 * Copyright 2019 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.dataloaders.rdbms;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.NotCondition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.graphql.query.Filter;
import com.liaison.dataquery.graphql.query.FilterSet;
import io.jsonwebtoken.lang.Collections;

import java.util.Collection;
import java.util.List;

public abstract class SQLQueryParser {

    public static final String DEFAULT_ID = "id";

    public static String doubleQuote(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return "\"" + s + "\"";
    }

    static String singleQuote(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return "\'" + s + "\'";
    }

    static Condition parseFilterSet(DbTable table, FilterSet filterSet) {
        return parseFilterSet(table, filterSet, null, null);

    }

    static Condition parseFilterSet(DbTable table, FilterSet filterSet, SQLQuery sqlQuery, SQLAliasCache aliasCache) {
        if (filterSet == null) {
            return null;
        }
        ComboCondition comboCondition = null;
        if (filterSet.getLogicalOperator() != null) {
            comboCondition = new ComboCondition(ComboCondition.Op.valueOf(filterSet.getLogicalOperator().name()));
        }
        if (filterSet.getFilters() != null) {
            // If filterset.filters array has elements parse those
            if (comboCondition != null) {
                addFilters(table, filterSet.getFilters(), comboCondition, sqlQuery, aliasCache);
            } else {
                return getBinaryOrInCondition(table, filterSet.getFilters().get(0), sqlQuery, aliasCache);
            }
        } else {
            // Filterset must have nested filtersets, parse those
            for (FilterSet f : filterSet.getFiltersets()) {
                Condition filterCombo = parseFilterSet(table, f, sqlQuery, aliasCache);
                if (comboCondition != null) {
                    comboCondition.addCondition(filterCombo);
                } else {
                    // FilterSet has no logical op, can not addTable to combo -> return a single condition
                    return filterCombo;
                }
            }
        }
        if (filterSet.isNegation()) {
            return new NotCondition(comboCondition);
        }
        return comboCondition;
    }

    private static void addFilters(DbTable table, List<Filter> filters, ComboCondition comboCondition, SQLQuery sqlQuery, SQLAliasCache aliasCache) {
        for (Filter filter : filters) {
            Condition binaryCondition = getBinaryOrInCondition(table, filter, sqlQuery, aliasCache);
            comboCondition.addCondition(binaryCondition);
        }
    }


    private static Condition getBinaryOrInCondition(DbTable rootTable, Filter filter,
                                                    SQLQuery sqlQuery, SQLAliasCache aliasCache) {
        DbTable table = rootTable;
        if (aliasCache != null && !Collections.isEmpty(filter.getPath())) {
            String alias = aliasCache.getAlias(filter.getPath());
            table = aliasCache.getTable(alias);
        }
        if (FilterSet.ComparisonOperator.IN.equals(filter.getComparisonOperator())) {
            String inVars = null;
            if (filter.getValue() instanceof java.util.Collection) {
                Collection collectionValues = (Collection) filter.getValue();
                for (Object colVal : collectionValues) {
                    if (inVars == null) {
                        inVars = "?";
                    } else {
                        inVars += ", ?";
                    }
                    if (sqlQuery != null) {
                        sqlQuery.addParameter(colVal);
                    }
                }
            }
            InCondition inCondition = new InCondition(new DbColumn(table, filter.getQuotedFieldName(), null), new CustomSql(inVars));
            inCondition.setNegate(filter.isNegation());
            return inCondition;
        }
        BinaryCondition.Op op = toRDBMSComparison(filter.getComparisonOperator());
        if (filter.isID() && filter.getFieldName() == null) {
            filter.setFieldName("id");
        }
        Condition binaryCondition = new BinaryCondition(op, new DbColumn(table, filter.getQuotedFieldName(), filter.getFieldType()), new CustomSql("?"));
        if (sqlQuery != null) {
            sqlQuery.addParameter(filter.getValue());
        }
        if (filter.isNegation()) {
            binaryCondition = new NotCondition(binaryCondition);
        }
        return binaryCondition;
    }

    private static BinaryCondition.Op toRDBMSComparison(FilterSet.ComparisonOperator comparisonOperator) {
        switch (comparisonOperator) {
            case EQ:
                return BinaryCondition.Op.EQUAL_TO;
            case NE:
                return BinaryCondition.Op.NOT_EQUAL_TO;
            case GE:
                return BinaryCondition.Op.GREATER_THAN_OR_EQUAL_TO;
            case GT:
                return BinaryCondition.Op.GREATER_THAN;
            case LE:
                return BinaryCondition.Op.LESS_THAN_OR_EQUAL_TO;
            case LT:
                return BinaryCondition.Op.LESS_THAN;
            case LIKE:
                return BinaryCondition.Op.LIKE;
            default:
                throw new DataqueryRuntimeException("Unknown comparison operator.");
        }
    }
}
