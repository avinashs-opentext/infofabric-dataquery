/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.query;

import com.healthmarketscience.sqlbuilder.OrderObject;

import java.util.Objects;

import static com.liaison.dataquery.util.DataqueryUtils.addQuotes;

public class Sort {
    private String field;
    private final OrderObject.Dir direction;

    public Sort(String field, OrderObject.Dir direction) {
        this.field = field;
        this.direction = direction;
    }

    public String getField() {
        return field;
    }

    public String getQuotedField() {
        return addQuotes(field);
    }

    public OrderObject.Dir getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return "Sort{" + "direction='" + direction + '\'' + ", field='" + field + '\'' + '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, field);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sort sort = (Sort) o;
        return Objects.equals(field, sort.field) &&
                direction == sort.direction;
    }

    public void setField(String field) {
        this.field = field;
    }
}
