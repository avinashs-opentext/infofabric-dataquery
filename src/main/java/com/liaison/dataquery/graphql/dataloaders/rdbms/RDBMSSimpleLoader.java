/**
 * Copyright 2019 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.dataloaders.rdbms;

import com.liaison.datagate.rdbms.sql.MVPreparedStatement;
import com.liaison.dataquery.exception.DataLoaderException;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.graphql.dataloaders.DataQuerySimpleLoader;
import com.liaison.dataquery.graphql.query.Query;
import com.liaison.dataquery.graphql.results.ResultList;
import com.liaison.dataquery.graphql.results.ResultObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class RDBMSSimpleLoader extends RDBMSLoader implements DataQuerySimpleLoader {

    @Override
    public ResultList load(Query query) {
        SQLQuery sql = new SQLSimpleParser().parse(query);
        String tenant = query.getCollection().getTenant();
        final RDBMSConnectionProvider connectionProvider = RDBMSConnectionProvider.forTenant(tenant);

        ResultSet resultSet = null;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql.getSql());
             MVPreparedStatement mvPreparedStatement = new MVPreparedStatement(preparedStatement)) {

            for (int i = 0; i < sql.getParameters().size(); ++i) {
                mvPreparedStatement.setObject(i + 1, sql.getParameters().get(i));
            }
            resultSet = mvPreparedStatement.executeQuery();
            return resultSetToList(resultSet, sql);

        } catch (Exception e) {
            throw new DataLoaderException(e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    // Ignore.
                }
            }
        }

    }

    // Internal helper classes for parsing the column structure into object tree.
    //
    //           1--1 Column (primary key)
    // RowObject 1--* Column (value columns)
    //           1--* RowObject (children)
    class RowObject {
        String parentPath;
        String parentField;
        String path;
        Column keyColumn;
        List<Column> valueColumns = new ArrayList<>();
        List<RowObject> children = new ArrayList<>();

        RowObject(String path) {
            this.path = path;
            int i = path.lastIndexOf('.');
            if (i > 0) {
                this.parentField = path.substring(i + 1);
                this.parentPath = path.substring(0, i);
            }
        }
    }

    class Column {
        final String name;
        final String label;

        Column(String label, String name) {
            this.label = label;
            this.name = name;
        }
    }

    private ResultList resultSetToList(ResultSet rs, SQLQuery sqlQuery) throws SQLException {
        rs.setFetchSize(100);
        final ResultList resultTreeList = new ResultList();
        // RowObjects are a representation of the table in a tree structure
        final RowObject root = parseColumnTree(rs.getMetaData(), sqlQuery.getAliasCache());
        // ObjectCache is used when there is cartesian product and the same object instance may be present multiple times
        final Map<Object, ResultObject> objectCache = new HashMap<>();
        while (rs.next()) {
            if (sqlQuery.cartesian()) {
                parseTabularTree(root, rs, resultTreeList, sqlQuery.getAliasCache(), objectCache);
            } else {
                parseTable(root, rs, resultTreeList, sqlQuery.getAliasCache(), null);
            }
        }
        return resultTreeList;
    }

    // For parsing a resultset where each row is a new object, for example aggregates
    private void parseTable(RowObject r, ResultSet rs, ResultList resultTreeList, SQLAliasCache aliasCache, ResultObject parent) throws SQLException {
        ResultObject object = new ResultObject();
        if (r.keyColumn != null) {
            object.put(r.keyColumn.name, rs.getObject(r.keyColumn.label));
        }
        for (Column c : r.valueColumns) {
            if (rs.getObject(c.label) instanceof java.sql.Time) {
                object.put(c.name, rs.getObject(c.label, LocalTime.class));
            } else {
                object.put(c.name, rs.getObject(c.label));
            }
        }
        if (resultTreeList != null) {
            // resultTreeList is non null only for root object
            resultTreeList.add(object);
        }
        if (parent != null) {
            if (aliasCache.hasMany(r.path)) {
                parent.putIfAbsent(r.parentField, new ArrayList<>());
                ((List) parent.get(r.parentField)).add(object);
            } else {
                parent.put(r.parentField, object);
            }
        }
        for (RowObject child : r.children) {
            parseTable(child, rs, null, aliasCache, object);
        }
    }

    // For parsing a resultset where the same object instance may appear multiple times on row due to cartesian product joins
    private ResultObject parseTabularTree(RowObject r,
                                          ResultSet rs,
                                          ResultList resultTreeList,
                                          SQLAliasCache aliasCache,
                                          Map<Object, ResultObject> objectCache) throws SQLException {
        final Object objectKey = rs.getObject(r.keyColumn.label);
        if (objectKey == null) {
            // Object PK is null -> this row does not have data for this type of object
            return null;
        }
        final String cacheKey = getCacheKey(r.path, r.keyColumn.name, objectKey);
        ResultObject object;
        if (!objectCache.containsKey(cacheKey)) {
            object = new ResultObject(cacheKey);
            object.put(r.keyColumn.name, objectKey);
            objectCache.put(cacheKey, object);
            if (resultTreeList != null) {
                // resultTreeList is non null only for root object
                resultTreeList.add(object);
            }
        } else {
            object = objectCache.get(cacheKey);
        }

        for (Column c : r.valueColumns) {
            if (!object.containsKey(c.name) || object.get(c.name) == null) {
                if (rs.getObject(c.label) instanceof java.sql.Time) {
                    object.put(c.name, rs.getObject(c.label, LocalTime.class));
                } else {
                    object.put(c.name, rs.getObject(c.label));
                }
            }
        }

        for (RowObject child : r.children) {
            ResultObject childObject = parseTabularTree(child, rs, null, aliasCache, objectCache);
            if (!object.containsKey(child.parentField) && aliasCache.hasMany(child.path)) {
                object.put(child.parentField, new TreeSet<>());
            }
            if (childObject == null) {
                continue;
            }
            if (aliasCache.hasMany(child.path)) {
                Set set = (Set) object.get(child.parentField);
                set.add(childObject);
            } else {
                object.put(child.parentField, childObject);
            }
        }
        return object;
    }

    private RowObject parseColumnTree(ResultSetMetaData md, SQLAliasCache aliasCache) throws SQLException {
        final int columns = md.getColumnCount();
        final SortedMap<String, RowObject> objectMap = new TreeMap<>();
        RowObject root = null;
        for (int i = 1; i <= columns; ++i) {
            final String columnLabel = md.getColumnLabel(i);
            final String tableAlias = columnLabel.split("\\.")[0];
            final String fieldName = columnLabel.split("\\.")[1];
            final String tablePath = aliasCache.getByAlias(tableAlias);
            if (!objectMap.containsKey(tablePath)) {
                RowObject rowObject = new RowObject(tablePath);
                objectMap.put(tablePath, rowObject);
                if (tableAlias.equals(aliasCache.getRootObjectAlias())) {
                    root = rowObject;
                }
            }
            RowObject rowObject = objectMap.get(tablePath);
            Column column = new Column(columnLabel, fieldName);
            if (fieldName.equals(aliasCache.getTablePathKey(tablePath))) {
                rowObject.keyColumn = column;
            } else {
                rowObject.valueColumns.add(column);
            }
        }

        if (root == null) {
            throw new DataqueryRuntimeException("No root object found in result table.");
        }

        objectMap.forEach((path, rowObject) -> {
            if (rowObject.parentPath != null && objectMap.containsKey(rowObject.parentPath)) {
                objectMap.get(rowObject.parentPath).children.add(rowObject);
            }
        });

        return root;
    }

    private String getCacheKey(String tablePath, String keyField, Object keyValue) {
        return String.format("%s.%s=%s", tablePath, keyField, keyValue);
    }
}

