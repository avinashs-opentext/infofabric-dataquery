/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.liaison.dataquery.util.DataqueryUtils.addQuotes;

public class Projection {
    private final String dataType;
    private final String field;
    private final String alias;
    private final boolean autoProjection;

    Projection(String field, String alias, String dataType, boolean auto) {
        this.field = field;
        this.alias = alias;
        this.dataType = dataType;
        this.autoProjection = auto;
    }

    public String getField() {
        return field;
    }

    public String getQuotedField() {
        return addQuotes(field);
    }

    public String getAlias() {
        return alias;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isAutoProjection() {
        return autoProjection;
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof Projection)) {
            return false;
        }
        Projection other = (Projection) obj;
        return (this.alias != null ? this.alias.equals(other.getAlias()) : other.alias == null)
                && this.field.equals(other.getField())
                && ((this.getDataType() == null && other.getDataType() == null) ||
                (this.getDataType().equals(other.getDataType())));

    }

    @Override
    public int hashCode() {
        return Objects.hash(field, dataType, alias);
    }

    @Override
    public String toString() {
        List<String> fullName = new ArrayList<String>();
        fullName.add(field);
        return String.join(".", fullName) + (alias != null ? " as " + alias : " ") + "(" + dataType + ")";
    }
}