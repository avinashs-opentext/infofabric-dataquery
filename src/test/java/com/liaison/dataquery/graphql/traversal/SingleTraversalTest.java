/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.traversal;

import com.liaison.dataquery.dto.AccessPrivileges;
import com.liaison.dataquery.graphql.GraphQLService;
import com.liaison.dataquery.graphql.RootDataAccess;
import com.liaison.dataquery.graphql.query.Filter;
import com.liaison.dataquery.graphql.query.FilterSet;
import com.liaison.dataquery.graphql.query.QueryContext;
import graphql.ExecutionInput;
import graphql.language.FieldDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SingleTraversalTest {

    public void beforeTest() {
        SingleTraversal st = new SingleTraversal();


        // query context
        Set<String> readPrivilegesTableSet = new HashSet<>();
        readPrivilegesTableSet.addAll(Arrays.asList("Book", "Author"));
        Set<String> writePrivilegesTableSet = new HashSet<>();
        writePrivilegesTableSet.addAll(Arrays.asList("Book", "Author"));
        Map<String, FilterSet> rowSecurityMap = new HashMap<>();
        FilterSet rootFilterSet = new FilterSet();
        rootFilterSet.initFiltersets();

        FilterSet childFilterSet = new FilterSet();
        childFilterSet.initFiltersets();
        childFilterSet.getFilters().add(new Filter("name","", FilterSet.ComparisonOperator.EQ,"Hadley Ruggier", false));
        childFilterSet.setLogicalOperator(FilterSet.LogicalOperator.AND);

        rootFilterSet.getFiltersets().add(childFilterSet);
        rootFilterSet.setLogicalOperator(FilterSet.LogicalOperator.OR);

        rowSecurityMap.put("Author", rootFilterSet);
        AccessPrivileges ap = new AccessPrivileges("unittesttenant", "basic_multi_table_01", readPrivilegesTableSet, writePrivilegesTableSet, rowSecurityMap);
        QueryContext queryContext = new QueryContext(RootDataAccess.QueryModel.SINGLE_QUERY, "thermofisher", "thermo_ref_ui", "master", GraphQLService.ViewType.RDBMS, null, null, ap);

        // rootType
        String query = "{\n" +
                "    Author_by_id(id: 4)\n" +
                "    {\n" +
                "        name\n" +
                "        isAnyGood\n" +
                "    }\n" +
                "    Book_by_id(id: 11)\n" +
                "    {\n" +
                "        title\n" +
                "        language\n" +
                "    }\n" +
                "}";
        ExecutionInput input = new ExecutionInput(query, null, queryContext, null, null);
        List<GraphQLFieldDefinition> fieldDefinitionList = new ArrayList<>();
//        fieldDefinitionList.add(new GraphQLFieldDefinition("authorId"));
//        fieldDefinitionList.add(new GraphQLFieldDefinition("name"));
//        fieldDefinitionList.add(new GraphQLFieldDefinition("isAnyGood"));
        GraphQLObjectType rootType = new GraphQLObjectType("Author", "",fieldDefinitionList, null);
    }
}
