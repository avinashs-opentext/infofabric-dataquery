/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.dto;

import com.liaison.dataquery.graphql.query.FilterSet;

public class DmFilter {
    private String column;

    private String op;

    private String value;

    public DmFilter() {
    }

    public DmFilter(String column, String value) {
        this.column = column;
        this.value = value;
        this.op = FilterSet.ComparisonOperator.EQ.name();
    }

    public DmFilter(String column, String op, String value) {
        this.column = column;
        this.op = op;
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "{ field='" + column + '\'' +
                " op='" + op + '\'' +
                " value='" + value + '\'' +
                '}';
    }
}
