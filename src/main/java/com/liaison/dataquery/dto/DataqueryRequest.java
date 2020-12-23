/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.dto;

import java.util.Map;

public class DataqueryRequest extends DataqueryMeta {

    private String query;
    private String operationName;
    private Map<String, Object> variables;
    boolean enableInstrumentation = false;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public boolean isEnableInstrumentation() {
        return enableInstrumentation;
    }

    public void setEnableInstrumentation(boolean enableInstrumentation) {
        this.enableInstrumentation = enableInstrumentation;
    }
}
