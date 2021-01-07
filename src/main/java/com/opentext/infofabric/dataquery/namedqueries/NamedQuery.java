/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.namedqueries;

import com.opentext.infofabric.dataquery.graphql.GraphQLService;
import graphql.ExecutionResult;

import java.util.Map;

public interface NamedQuery {
    Object preview(String tenant, String datamodel, Map<String, Object> variables);

    ExecutionResult execute(String tenant, String datamodel, Map<String, Object> variables);

    String getDescription();

    String getName();

    GraphQLService.ViewType getType();

    Map<String, String> getVariables();
}
