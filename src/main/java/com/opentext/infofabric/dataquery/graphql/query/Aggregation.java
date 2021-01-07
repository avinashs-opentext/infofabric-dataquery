/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.query;

public class Aggregation {

    private final String type;

    public Aggregation(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
