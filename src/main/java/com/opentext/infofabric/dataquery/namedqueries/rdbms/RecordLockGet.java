/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.namedqueries.rdbms;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.UpdateQuery;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms.SQLBatchParser;
import graphql.ExecutionResult;

import java.util.HashMap;
import java.util.Map;

public class RecordLockGet extends NamedUpdateStatement {

    private static final String LOCK_ID_COL = SQLBatchParser.doubleQuote("lock_id");
    private static final String LOCK_AT_COL = SQLBatchParser.doubleQuote("lock_at");
    private static final String LOCK_EXP_COL = SQLBatchParser.doubleQuote("lock_exp");
    private static final String LOCK_READ_COL = SQLBatchParser.doubleQuote("lock_read");

    static final String TABLE_VAR = "table";
    static final String KEY_VAR = "key";
    static final String KEYCOL_VAR = "keyColumn";
    static final String TTL_VAR = "ttl";
    static final String VALUE_VAR = "value";
    static final String READ_VAR = "readOnly";
    static final String OLD_VALUE_VAR = "oldValue";
    String setNow = "now()";

    String queryName = "pgGetRowLock";
    String description = "Sets the value of lock_id column if not already set to a different non-null value or if the current lock is expired." +
            " Sets current timestamp to lock_at column when aquiring the lock and the expiration time to lock_exp column. Returns 1 if lock is acquired, 0 otherwise." +
            " NOTE: Nullable columns lock_id (text), lock_at (timestampz), lock_exp (timestampz) and lock_read (boolean) must to be present.";

    static final String MISSING_ALL_VARS_ERROR = "Missing variables from the query.";
    static final String MISSING_VAR_ERROR = "Missing variable: %s.";

    @Override
    public ExecutionResult execute(String tenant, String datamodel, Map<String, Object> variables) {
        return executeQuery(tenant, build(datamodel, variables));
    }

    @Override
    public Object preview(String tenant, String datamodel, Map<String, Object> variables) {
        return build(datamodel, variables);
    }

    protected void validate(Map<String, Object> variables) {
        if (variables == null) {
            throw new DataqueryRuntimeException(MISSING_ALL_VARS_ERROR);
        }
        if (!variables.containsKey(TABLE_VAR)) {
            throw new DataqueryRuntimeException(String.format(MISSING_VAR_ERROR, TABLE_VAR));
        }
        if (!variables.containsKey(VALUE_VAR)) {
            throw new DataqueryRuntimeException(String.format(MISSING_VAR_ERROR, VALUE_VAR));
        }
        if (!variables.containsKey(TTL_VAR)) {
            throw new DataqueryRuntimeException(String.format(MISSING_VAR_ERROR, TTL_VAR));
        }
        if (!variables.containsKey(KEY_VAR)) {
            throw new DataqueryRuntimeException(String.format(MISSING_VAR_ERROR, KEY_VAR));
        }
        if (!variables.containsKey(KEYCOL_VAR)) {
            throw new DataqueryRuntimeException(String.format(MISSING_VAR_ERROR, KEYCOL_VAR));
        }
        if (!variables.containsKey(READ_VAR)) {
            throw new DataqueryRuntimeException(String.format(MISSING_VAR_ERROR, READ_VAR));
        }

        variables.put(OLD_VALUE_VAR, variables.get(VALUE_VAR));
    }

    private String build(String datamodel, Map<String, Object> variables) {

        validate(variables);

        String tableName = SQLBatchParser.doubleQuote((String) variables.get(TABLE_VAR));
        String keyColName = SQLBatchParser.doubleQuote((String) variables.getOrDefault(KEYCOL_VAR, "ID"));

        DbSpec dbSpec = new DbSpec();
        DbSchema dbSchema = new DbSchema(dbSpec, SQLBatchParser.doubleQuote(datamodel));
        DbTable table = new DbTable(dbSchema, tableName);
        DbColumn valueCol = new DbColumn(table, LOCK_ID_COL, "text");
        DbColumn tsCol = new DbColumn(table, LOCK_AT_COL, "timestampz");
        DbColumn expiresAtCol = new DbColumn(table, LOCK_EXP_COL, "int");
        DbColumn readCol = new DbColumn(table, LOCK_READ_COL, "boolean");
        DbColumn keyCol = new DbColumn(table, keyColName, "bigint");

        UpdateQuery updateQuery = new UpdateQuery(table);
        updateQuery.addSetClause(valueCol, variables.get(VALUE_VAR));
        updateQuery.addSetClause(readCol, variables.get(READ_VAR) != null ? String.valueOf(variables.get(READ_VAR)) : null); // Wrap boolean as string for pg

        CustomSql epoch = new CustomSql(setNow);
        updateQuery.addCustomSetClause(tsCol, epoch);

        String expiresStr = buildExpiresAtSql(variables.get(TTL_VAR));
        CustomSql expires = new CustomSql(expiresStr);
        updateQuery.addSetClause(expiresAtCol, expires);

        ComboCondition valueOR = new ComboCondition(ComboCondition.Op.OR);
        BinaryCondition valueEQ = new BinaryCondition(BinaryCondition.Op.EQUAL_TO, valueCol, variables.get(OLD_VALUE_VAR));
        BinaryCondition valueNULL = new BinaryCondition(" IS ", valueCol, SqlObject.NULL_VALUE);
        valueOR.addConditions(valueEQ, valueNULL);

        ComboCondition tsOR = new ComboCondition(ComboCondition.Op.OR);
        CustomSql compareNow = new CustomSql("now()");
        BinaryCondition tsLT = new BinaryCondition(BinaryCondition.Op.LESS_THAN, expiresAtCol, compareNow);
        BinaryCondition tsNULL = new BinaryCondition(" IS ", tsCol, SqlObject.NULL_VALUE);
        tsOR.addConditions(tsLT, tsNULL);

        BinaryCondition keyEQ = new BinaryCondition(BinaryCondition.Op.EQUAL_TO, keyCol, variables.get(KEY_VAR));

        ComboCondition valueTsOR = new ComboCondition(ComboCondition.Op.OR);
        valueTsOR.addConditions(valueOR, tsOR);

        ComboCondition and = new ComboCondition(ComboCondition.Op.AND);
        and.addConditions(keyEQ, valueTsOR);

        updateQuery.addCondition(and);

        return updateQuery.validate().toString();
    }

    protected String buildExpiresAtSql(Object ttl) {
        return "now() + interval '" + ttl + "'";
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return queryName;
    }

    @Override
    public Map<String, String> getVariables() {
        Map<String, String> vars = new HashMap();
        vars.put(TABLE_VAR, "Table name, string.");
        vars.put(VALUE_VAR, "Lock object value, string.");
        vars.put(TTL_VAR, "Lock TTL in seconds, int.");
        vars.put(KEY_VAR, "Row primary key value, any.");
        vars.put(KEYCOL_VAR, "Primary key column name, string.");
        vars.put(READ_VAR, "Is read lock, boolean.");
        return vars;
    }

}
