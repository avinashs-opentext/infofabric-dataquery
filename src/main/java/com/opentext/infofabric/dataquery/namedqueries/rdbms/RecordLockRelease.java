/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.namedqueries.rdbms;

import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;

import java.util.HashMap;
import java.util.Map;

public class RecordLockRelease extends RecordLockGet {
    public RecordLockRelease() {
        setNow = "NULL";
        queryName = "pgReleaseRowLock";
        description = "Releases the lock acquired by pgGetRowLock. Returns 1 if lock was released, 0 if not " +
                "(lock was expired and acquired by another process).";
    }

    @Override
    protected String buildExpiresAtSql(Object ttl) {
        return "NULL";
    }

    @Override
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
        if (!variables.containsKey(KEY_VAR)) {
            throw new DataqueryRuntimeException(String.format(MISSING_VAR_ERROR, KEY_VAR));
        }
        if (!variables.containsKey(KEYCOL_VAR)) {
            throw new DataqueryRuntimeException(String.format(MISSING_VAR_ERROR, KEYCOL_VAR));
        }
        variables.put(OLD_VALUE_VAR, variables.get(VALUE_VAR));
        variables.put(VALUE_VAR, null);
        variables.put(TTL_VAR, null);
        variables.put(READ_VAR, null);
    }

    @Override
    public Map<String, String> getVariables() {
        Map<String, String> vars = new HashMap();
        vars.put(TABLE_VAR, "Table name, string.");
        vars.put(VALUE_VAR, "Released lock value, string.");
        vars.put(KEY_VAR, "Row primary key value, any.");
        vars.put(KEYCOL_VAR, "Primary key column name, string.");
        return vars;
    }

}
