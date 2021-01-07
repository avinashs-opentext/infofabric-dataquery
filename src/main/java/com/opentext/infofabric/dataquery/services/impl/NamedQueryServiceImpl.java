/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.services.impl;

import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.GraphQLService;
import com.opentext.infofabric.dataquery.services.NamedQueryService;
import com.opentext.infofabric.dataquery.namedqueries.NamedQuery;
import com.opentext.infofabric.dataquery.namedqueries.rdbms.RecordLockGet;
import com.opentext.infofabric.dataquery.namedqueries.rdbms.RecordLockRelease;
import graphql.ExecutionResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NamedQueryServiceImpl implements NamedQueryService {

    Map<String, NamedQuery> namedQueries = new HashMap<>();

    public NamedQueryServiceImpl() {
        RecordLockGet recordLockGet = new RecordLockGet();
        namedQueries.put(recordLockGet.getName(), recordLockGet);

        RecordLockRelease recordLockRelease = new RecordLockRelease();
        namedQueries.put(recordLockRelease.getName(), recordLockRelease);
    }

    @Override
    public ExecutionResult execute(String tenant, String datamodel, String queryName, Map<String, Object> variables) {
        if (!namedQueries.containsKey(queryName)) {
            throw new DataqueryRuntimeException(String.format("No query found for name %s.", queryName));
        }
        return namedQueries.get(queryName).execute(tenant, datamodel, variables);
    }

    @Override
    public Object preview(String tenant, String datamodel, String queryName, Map<String, Object> variables) {
        if (!namedQueries.containsKey(queryName)) {
            throw new DataqueryRuntimeException(String.format("No query found for name %s.", queryName));
        }
        return namedQueries.get(queryName).preview(tenant, datamodel, variables);
    }

    @Override
    public NamedQuery get(String queryName) {
        return namedQueries.get(queryName);
    }

    @Override
    public Set<String> getNames(GraphQLService.ViewType viewType) {
        Set<String> names = new HashSet<>();
        namedQueries.forEach((name, namedQuery) -> {
            if (namedQuery.getType().equals(viewType)) {
                names.add(name);
            }
        });
        return names;
    }
}
