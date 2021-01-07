/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql;

import com.opentext.infofabric.dataquery.dto.AccessPrivileges;
import graphql.ExecutionResult;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.opentext.infofabric.dataquery.DataqueryConstants.READ_PRIVILEGES;
import static com.opentext.infofabric.dataquery.graphql.TestHelper.GetService;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Test
public class GenericDataFetcherTest {

    private AccessPrivileges accessPrivileges ;

    @BeforeTest
    public void init(){
        //Mocking object security
        accessPrivileges = mock(AccessPrivileges.class) ;
        doReturn(true).when(accessPrivileges).isAuthorized("Person", READ_PRIVILEGES);
        doReturn(true).when(accessPrivileges).isAuthorized("Address", READ_PRIVILEGES);
    }

    @Test
    public void SimpleQueryTest_0() {
        String qry = "{ Person_query( filterset: {filters : [" +
                "{ field: \"name\" op : EQ value: \"John Doe\"}" +
                "]} ) { name age } }";
        Map result = GetService().execute("","test", "person", "master",
                qry, null, GraphQLService.ViewType.MOCK, RootDataAccess.QueryModel.MULTI_QUERY, accessPrivileges).getData();
        Assert.assertTrue(result.containsKey("Person_query"));
        List<Map<String, Object>> person_query = (List<Map<String, Object>>) result.get("Person_query");
        Assert.assertTrue(person_query.get(0).containsKey("name"));
        Assert.assertTrue(person_query.get(0).containsKey("age"));
        Assert.assertTrue(person_query.get(0).get("age") instanceof Integer);
    }

    @Test
    public void SimpleQueryTest_1() {
        String qry = "{ Person_query( filterset: {filters : [" +
                "{ field: \"name\" op : EQ value: \"John Doe\"}, " +
                "{ field: \"name\" op : EQ value: \"Jane Doe\"}" +
                "] op: OR} ) { name age } }";

        Map result = GetService().execute("","test", "person", "master",
                qry, null, GraphQLService.ViewType.MOCK, RootDataAccess.QueryModel.MULTI_QUERY, accessPrivileges).getData();
        Assert.assertTrue(result.containsKey("Person_query"));
        List<Map<String, Object>> person_query = (List<Map<String, Object>>) result.get("Person_query");
        Assert.assertTrue(person_query.get(0).containsKey("name"));
        Assert.assertTrue(person_query.get(0).containsKey("age"));
        Assert.assertTrue(person_query.get(0).get("age") instanceof Integer);
    }

    @Test
    public void SimpleByIdTest_1() {
        String qry = "{ Person_by_id( id : [0 1] ) {name age favoriteColor} }";
        GetService().execute("","test", "person", "master", qry, GraphQLService.ViewType.MOCK, new AccessPrivileges());
    }

    @Test
    public void InvalidQueryTest() {
        String qry = "{ Person_query( filterset: {filters : [" +
                "{ field: \"name\" op : EQ value: \"John Doe\"}, " +
                "{ field: \"name\" op : EQ value: \"Jane Doe\"}" +
                "]} ) { name age } }";
        // Missing logical operator (op: OR)
        List errors = GetService().execute("","test", "person", "master", qry, GraphQLService.ViewType.MOCK, new AccessPrivileges()).getErrors();
        Assert.assertEquals(errors.size(), 1);
    }

    @Test
    public void NestedQueryTest() {
        String query = "{ Person_query( filterset: { " +
                "filtersets: [" +
                "{ filters : [" +
                " { field: \"name\" op : EQ value: \"John Doe\"}, " +
                " { field: \"name\" op : EQ value: \"Jane Doe\"}" +
                "] op: OR},  " +
                "{ filters : [" +
                " { field: \"age\" op : LT value: \"40\"}, " +
                " { field: \"age\" op : GT value: \"20\"}" +
                "] op: AND},  " +
                "{ filtersets: [" +
                "  { filters : [" +
                "   { not: true field: \"name\" op : EQ value: \"John Smith\"}, " +
                "   { field: \"name\" op : EQ value: \"Jane Smith\"}" +
                "  ] op: OR},  " +
                "  { filters : [" +
                "   { field: \"age\" op : LT value: \"70\"}, " +
                "   { field: \"age\" op : GT value: \"50\"}" +
                "  ] op: AND}  " +
                "], op: AND} " +
                "], op: AND} " +
                ") { name favoriteColor } }";

        Map result = GetService().execute("","test", "person", "master", query, null, GraphQLService.ViewType.MOCK,
                RootDataAccess.QueryModel.MULTI_QUERY, accessPrivileges).getData();
        Assert.assertTrue(result.containsKey("Person_query"));
        List<Map<String, Object>> person_query = (List<Map<String, Object>>) result.get("Person_query");
        Assert.assertTrue(person_query.get(0).containsKey("name"));
        Assert.assertTrue(person_query.get(0).containsKey("favoriteColor"));
    }

    @Test
    public void ComplexObjectTest_1() {
        String qry = "{ Person_query( filterset: {filters : [" +
                "{ field: \"name\" op : EQ value: \"John Doe\"}, " +
                "{ field: \"name\" op : EQ value: \"Jane Doe\"}" +
                "] op: OR} ) { name age addresses( filterset: {filters : [" +
                "                { field: \"state\" op : EQ value: \"GA\" }" +
                "                ]} ) { street owner {name} } } }";

        ExecutionResult executionResult = GetService().execute("","test", "person",
                "master", qry, null, GraphQLService.ViewType.MOCK, RootDataAccess.QueryModel.MULTI_QUERY, accessPrivileges);
        Map result = executionResult.getData();
        Assert.assertTrue(result.containsKey("Person_query"));
        List<Map<String, Object>> person_query = (List<Map<String, Object>>) result.get("Person_query");
        Assert.assertTrue(person_query.get(0).containsKey("name"));
        Assert.assertTrue(person_query.get(0).containsKey("age"));
        Assert.assertTrue(person_query.get(0).get("age") instanceof Integer);
        Assert.assertTrue(person_query.get(0).get("addresses") instanceof List);
        // Check owner was flattened to a single map instead of an array
        Map<String, Object> address = ((List<Map<String, Object>>) person_query.get(0).get("addresses")).get(0);
        Assert.assertTrue(address.get("owner") instanceof Map);
    }

}
