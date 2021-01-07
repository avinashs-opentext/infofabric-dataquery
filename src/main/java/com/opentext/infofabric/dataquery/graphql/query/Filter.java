/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.query;


import com.opentext.infofabric.dataquery.util.DataqueryUtils;

import java.util.ArrayList;
import java.util.List;

public class Filter {

    private List<String> path = new ArrayList<>();
    private String fieldName;

    private String fieldType;
    private FilterSet.ComparisonOperator comparisonOperator;
    private Object value;
    private boolean negation;
    private boolean isID;


    public Filter(String fieldName, String fieldType, FilterSet.ComparisonOperator comparisonOperator,
                  Object value, boolean negation) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.comparisonOperator = comparisonOperator;
        this.value = value;
        this.negation = negation;
    }

    public Filter(String fieldName, String fieldType, FilterSet.ComparisonOperator comparisonOperator, Object value, boolean isNot, List<String> path) {
        this(fieldName, fieldType, comparisonOperator, value, isNot);
        this.path = path;
    }

    public boolean isNegation() {
        return negation;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getQuotedFieldName() {
        return DataqueryUtils.addQuotes(fieldName);
    }

    public String getQuotedFullName(String delimiter) {
        if (path == null || path.isEmpty()) {
            return getQuotedFieldName();
        }
        String joinedPath = String.join(delimiter, this.path);
        return String.format("%s%s%s", DataqueryUtils.addQuotes(joinedPath), delimiter, getQuotedFieldName());
    }

    public FilterSet.ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return (negation ? "NOT " : "") + "(" + fieldName + " " + comparisonOperator + " " + value + ")";
    }

    public void isID(boolean isID) {
        this.isID = isID;
    }

    public boolean isID() {
        return isID;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public List<String> getPath() {
        return path;
    }
}
