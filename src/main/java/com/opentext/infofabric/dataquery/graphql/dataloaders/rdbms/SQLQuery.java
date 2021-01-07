/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms;


import java.util.ArrayList;
import java.util.List;

public class SQLQuery {
    String sql;
    List<Object> parameters = new ArrayList<>();
    private SQLAliasCache aliasCache;
    private boolean isCartesian;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public void addParameter(Object parameter) {
        this.parameters.add(parameter);
    }


    public void setAliasCache(SQLAliasCache aliasCache) {
        this.aliasCache = aliasCache;
    }

    public SQLAliasCache getAliasCache() {
        return aliasCache;
    }

    public boolean cartesian() {
        return isCartesian;
    }

    public void setCartesian(boolean cartesian) {
        isCartesian = cartesian;
    }
}