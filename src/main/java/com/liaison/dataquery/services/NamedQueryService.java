/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.services;

import com.liaison.dataquery.graphql.GraphQLService;
import com.liaison.dataquery.namedqueries.NamedQuery;
import graphql.ExecutionResult;

import java.util.Map;
import java.util.Set;

public interface NamedQueryService {
    ExecutionResult execute(String tenant, String datamodel, String queryName, Map<String, Object> variables);

    Object preview(String tenant, String datamodel, String queryName, Map<String, Object> variables);

    NamedQuery get(String queryName);

    Set<String> getNames(GraphQLService.ViewType viewType);
}
