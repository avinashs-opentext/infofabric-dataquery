/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import com.opentext.infofabric.dataquery.graphql.query.JoinCondition;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SQLAliasCache {
    static final String DEFAULT_TABLE_ALIAS = "root";
    private String rootObjectAlias = null;
    private final Map<String, DbTable> tableByPath = new HashMap<>();
    private final Map<String, String> aliasMapping = new HashMap<>();
    private final Map<String, String> reverseAliasMapping = new HashMap<>();
    private final Map<String, String> tablePathKey = new HashMap<>();
    private final Map<String, Boolean> hasMany = new HashMap<>();

    synchronized String addAliasMapping(String name, boolean many) {
        if(aliasMapping.containsKey(name)){
            return aliasMapping.get(name);
        }
        String shortAlias = "d" + aliasMapping.size();
        if(rootObjectAlias == null){
            rootObjectAlias = shortAlias;
        }
        aliasMapping.put(name, shortAlias);
        reverseAliasMapping.put(shortAlias, name);
        hasMany.put(name, many);
        return shortAlias;
    }

    boolean hasMany(String name) {
        return hasMany.get(name);
    }

    void addTable(String fromName, DbTable fromTable) {
        tableByPath.put(fromName, fromTable);
    }

    DbTable getTable(String tablePath) {
        return tableByPath.get(tablePath);
    }

    String getAlias(String tablePath) {
        return aliasMapping.get(tablePath);
    }

    String getByAlias(String alias) {
        return reverseAliasMapping.get(alias);
    }

    String getTablePath(JoinCondition joinCondition) {
        return getTablePath(joinCondition, null);
    }

    String getTablePath(JoinCondition joinCondition, String bridge) {
        return getAlias(joinCondition.getPath(), bridge);
    }

    String getAlias(List<String> path) {
        return getAlias(path, null);
    }

    private String getAlias(List<String> path, String bridge) {
        String alias = DEFAULT_TABLE_ALIAS;
        if (Collections.isEmpty(path)) {
            return alias;
        } else {
            alias = String.join(".", path);
        }
        if (StringUtils.isNotEmpty(bridge)) {
            return String.format("%s.%s", alias, bridge);
        }
        return alias;
    }

    void addTablePathKey(String tablePath, String keyField) {
        tablePathKey.put(tablePath, keyField);
    }

    String getTablePathKey(String tablePath) {
        return tablePathKey.get(tablePath);
    }

    public String getRootObjectAlias() {
        return rootObjectAlias;
    }
}