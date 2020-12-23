/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.query;

import graphql.schema.GraphQLObjectType;

public class Collection {

    private final String name;
    private final String tenant;
    private final String datamodel;

    public Collection(String name, String tenant, String datamodel) {
        this.name = name;
        this.tenant = tenant;
        this.datamodel = datamodel;
    }

    public Collection(GraphQLObjectType currentType, QueryContext queryContext) {
        this.tenant = queryContext.getTenant();
        this.datamodel = queryContext.getDatamodel();
        this.name = currentType.getName();
    }

    public String getName() {
        return name;
    }

    public String getDatamodel() {
        return datamodel;
    }

    public String getTenant() {
        return tenant;
    }
}
