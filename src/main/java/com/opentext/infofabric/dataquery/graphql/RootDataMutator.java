/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql;

import com.google.inject.Inject;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.exception.DataCastManagerException;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.exception.MutationException;
import com.opentext.infofabric.dataquery.graphql.helpers.TypeMapper;
import com.opentext.infofabric.dataquery.graphql.mutation.TransactionService;
import com.opentext.infofabric.dataquery.graphql.query.QueryContext;
import com.opentext.infofabric.dataquery.graphql.results.ResultList;
import graphql.execution.DataFetcherResult;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class submits the data query mutation request to Data cast http endpoint.
 *
 * @author open text
 */
public class RootDataMutator implements RootDataAccess {
    private static final Logger log = LoggerFactory.getLogger(RootDataMutator.class);
    private static final String INPUT_ARGUMENT = "input";
    private static final String INSERT = "insert";
    private static final String UPSERT = "upsert";

    @Inject
    private static TransactionService transactionService;

    @Override
    public Object get(DataFetchingEnvironment environment) {
        // root field is the query method name
        String typeName;
        String mutationType;
        Field rootField = environment.getField();
        QueryContext queryContext = environment.getContext();
        queryContext.setQueryType(RootDataAccess.resolveQueryType(rootField.getName()));
        List<Map<String, Object>> inputArgument = environment.getArgument(INPUT_ARGUMENT);

        if (queryContext.getQueryType().equals(QueryType.INSERT)) {
            typeName = rootField.getName().replace("_insert", "");
            mutationType = INSERT;
        } else {
            typeName = rootField.getName().replace("_upsert", "");
            mutationType = UPSERT;
        }

        //Object security check starts here
        Set<String> allMutationTypes = new HashSet<>();
        allMutationTypes.add(typeName);
        GraphQLObjectType currentType = (GraphQLObjectType) environment.getGraphQLSchema().getType(typeName);
        traverseForAllTypes(currentType, inputArgument, allMutationTypes);
        if (log.isDebugEnabled()) {
            log.debug(allMutationTypes.toString());
        }

        if (!queryContext.getAccessPrivileges().isAuthorized(allMutationTypes, DataqueryConstants.WRITE_PRIVILEGES)) {
            throw new DataqueryRuntimeException(
                    String.format("User is not authorized for operation [%s], types %s",
                            mutationType, queryContext.getAccessPrivileges().getUnauthorizedTableNames(
                                    allMutationTypes, DataqueryConstants.WRITE_PRIVILEGES)));
        }

        Map<String, Object> map = new HashMap<>();
        List<JSONObject> list = new ArrayList<>();
        for (Map<String, Object> arg : inputArgument) {
            map.put(typeName, arg);
            JSONObject jsonObj = new JSONObject(map);
            list.add(jsonObj);
        }
        JSONArray jsArray = new JSONArray(list);
        HashMap<String, JSONArray> dcMap = new HashMap<>();
        dcMap.put(mutationType, jsArray);
        JSONObject dcJsonObj = new JSONObject(dcMap);
        if (log.isDebugEnabled()) {
            log.debug("Json request to DataCast--->" + dcJsonObj);
        }

        try {
            byte[] sendData = dcJsonObj.toString().getBytes(StandardCharsets.UTF_8);
            ResultList results = transactionService.upsert(queryContext, sendData);
            return new DataFetcherResult<>(results.get(0), new ArrayList<>());
        } catch (MutationException | DataCastManagerException e) {
            throw new DataqueryRuntimeException(e);
        }
    }

    /**
     * Traversal functions to get the types for the input json mutation request.
     *
     * @param currentType      - Graph QL object type for the current input
     * @param arguments        - input requests as arguments
     * @param allMutationTypes - set of all types used in mutation
     */
    private void traverseForAllTypes(GraphQLObjectType currentType,
                                     List<Map<String, Object>> arguments,
                                     Set<String> allMutationTypes) {
        arguments.forEach(arg -> traverseForAllTypes(currentType, arg, allMutationTypes));
    }

    /**
     * Traversal functions to get the types for the input json mutation request.
     *
     * @param parentType       - Graph QL object type for the parent input json request
     * @param argument         - input request as map argument
     * @param allMutationTypes - set of all types used in mutation
     */
    private void traverseForAllTypes(GraphQLObjectType parentType,
                                     Map<String, Object> argument,
                                     Set<String> allMutationTypes) {
        argument.keySet().forEach(key -> {
            Object value = argument.get(key);
            if (value instanceof Map) {
                GraphQLFieldDefinition fieldSDL = parentType.getFieldDefinition(key);
                GraphQLObjectType currentType = (GraphQLObjectType) TypeMapper.getWrappedType(fieldSDL.getType());
                allMutationTypes.add(currentType.getName());
                traverseForAllTypes(currentType, (Map) value, allMutationTypes);
            } else if (value instanceof List) {
                GraphQLFieldDefinition fieldSDL = parentType.getFieldDefinition(key);
                GraphQLObjectType currentType = (GraphQLObjectType) TypeMapper.getWrappedType(fieldSDL.getType());
                allMutationTypes.add(currentType.getName());
                traverseForAllTypes(currentType, (List) value, allMutationTypes);
            }
        });
    }
}