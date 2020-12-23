/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.rdbms;

import com.healthmarketscience.sqlbuilder.OrderObject;
import com.liaison.dataquery.graphql.RootDataAccess;
import com.liaison.dataquery.graphql.dataloaders.rdbms.SQLBatchParser;
import com.liaison.dataquery.graphql.query.Collection;
import com.liaison.dataquery.graphql.query.Filter;
import com.liaison.dataquery.graphql.query.FilterSet;
import com.liaison.dataquery.graphql.query.JoinCondition;
import com.liaison.dataquery.graphql.query.ProjectionSet;
import com.liaison.dataquery.graphql.query.Query;
import com.liaison.dataquery.graphql.query.Sort;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test
public class SQLParserTest {

    private Connection populateAndGetConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement stat = connection.createStatement();

            stat.executeUpdate("drop table if exists Person;");
            stat.executeUpdate("create table Person(id INT, firstName varchar(30), lastName varchar(30), age INT, spouse INT);");

            stat.executeUpdate("insert into Person values (0,'John','Doe',42,1);");
            stat.executeUpdate("insert into Person values (1,'Jane','Doe',40,0);");
            return connection;
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Sort nameSort = new Sort("firstName", OrderObject.Dir.ASCENDING);

    @Test
    public void SimpleTest_01() throws SQLException {
        ProjectionSet projectionSet = new ProjectionSet();
        projectionSet.addProjection("id", null, "Int");
        projectionSet.addProjection("firstName", null, "Int");
        projectionSet.addProjection("age", null, "Int");
        Query query = new Query(new Collection("Person", "tenant", null), projectionSet, null, 0L, 10L, nameSort, null);
        String sql = SQLBatchParser.parse(query);
        Connection connection = null;
        try {
            connection = populateAndGetConnection();
            Statement stat = connection.createStatement();
            ResultSet resultSet = stat.executeQuery(sql);
            List<Map<String, Object>> maps = resultSetToList(resultSet);
            Assert.assertEquals(maps.size(), 2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    @Test
    public void SimpleTest_02() throws SQLException {
        ProjectionSet projectionSet = new ProjectionSet();
        projectionSet.addProjection("id", null, "Int");
        projectionSet.addProjection("firstName", null, "Int");
        FilterSet filterSet = new FilterSet();
        filterSet.addFilter(new Filter("age", "Int", FilterSet.ComparisonOperator.GT, 40, false));
        Query query = new Query(new Collection("Person", "tenant", null), projectionSet, filterSet, 0L, 10L, nameSort, null);

        String sql = SQLBatchParser.parse(query);
        Connection connection = null;
        try {
            connection = populateAndGetConnection();
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setInt(1, Integer.parseInt(filterSet.getFilters().get(0).getValue().toString()));
            ResultSet resultSet = stat.executeQuery();
            List<Map<String, Object>> maps = resultSetToList(resultSet);
            Assert.assertEquals(maps.size(), 1);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    @Test
    public void JoinTest_01() throws SQLException {
        ProjectionSet projectionSet = new ProjectionSet();
        projectionSet.addProjection("id", null, "Int");
        projectionSet.addProjection("firstName", null, "Int");
        FilterSet filterSet = new FilterSet();
        filterSet.addFilter(new Filter("lastName", "String", FilterSet.ComparisonOperator.EQ, "Doe", false));

        JoinCondition jc1 = new JoinCondition(0, "spouse", "spouse", false);
        Query query1 = new Query(new Collection("Person", "tenant", null), projectionSet, filterSet, 0L, 10L, nameSort, null);
        query1.addJoinCondition(jc1);

        JoinCondition jc2 = new JoinCondition(1, "spouse", "spouse", false);
        Query query2 = new Query(new Collection("Person", "tenant", null), projectionSet, filterSet, 0L, 10L, nameSort, null);
        query2.addJoinCondition(jc2);

        List<Query> queries = new ArrayList<>();
        queries.add(query1);
        queries.add(query2);

        String sql = SQLBatchParser.parse(queries);
        Connection connection = null;
        try {
            connection = populateAndGetConnection();
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, filterSet.getFilters().get(0).getValue().toString());
            ResultSet resultSet = stat.executeQuery();
            List<Map<String, Object>> maps = resultSetToList(resultSet);
            Assert.assertEquals(maps.size(), 2);
            Assert.assertEquals(maps.get(0).get("firstName"), "Jane");
            Assert.assertEquals(maps.get(1).get("firstName"), "John");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
