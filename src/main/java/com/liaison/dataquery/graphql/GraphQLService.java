/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql;

import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.dto.AccessPrivileges;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.graphql.query.QueryContext;
import com.liaison.dataquery.guice.GuiceInjector;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.language.ScalarTypeDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.liaison.dataquery.DataqueryConstants.MUTATION_ROOT_TYPE;
import static com.liaison.dataquery.DataqueryConstants.QUERY_ROOT_TYPE;
import static com.liaison.dataquery.DataqueryConstants.SCALAR_ANY;
import static com.liaison.dataquery.DataqueryConstants.SCALAR_BASE64BINARY;
import static com.liaison.dataquery.DataqueryConstants.SCALAR_DATE;
import static com.liaison.dataquery.DataqueryConstants.SCALAR_TIME;
import static com.liaison.dataquery.DataqueryConstants.SCALAR_TIMESTAMP;
import static com.liaison.dataquery.graphql.CustomScalars.GraphQLAny;
import static com.liaison.dataquery.graphql.CustomScalars.GraphQLBase64Binary;
import static com.liaison.dataquery.graphql.CustomScalars.GraphQLDate;
import static com.liaison.dataquery.graphql.CustomScalars.GraphQLTime;
import static com.liaison.dataquery.graphql.CustomScalars.GraphQLTimestamp;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public class GraphQLService {

	private static final Logger log = LoggerFactory.getLogger(GraphQLService.class);
    public enum ViewType {RDBMS, MOCK, HBASE}
    
    
    private static RootDataAccess.QueryModel DEFAULT_QUERY_MODEL=GuiceInjector.getInjector().getInstance(RootDataAccess.QueryModel.class);;

    private ConcurrentHashMap<String, GraphQL> services = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> schemas = new ConcurrentHashMap<>();

    private static DataqueryConfiguration configuration = GuiceInjector.getInjector().getInstance(DataqueryConfiguration.class);

    public void createService(String tenant, String datamodel, String datamodelBranch, String sdl) {
    	SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(sdl);
        typeDefinitionRegistry.add(new ScalarTypeDefinition(SCALAR_TIMESTAMP));
        typeDefinitionRegistry.add(new ScalarTypeDefinition(SCALAR_DATE));
        typeDefinitionRegistry.add(new ScalarTypeDefinition(SCALAR_BASE64BINARY));
        typeDefinitionRegistry.add(new ScalarTypeDefinition(SCALAR_TIME));
        typeDefinitionRegistry.add(new ScalarTypeDefinition(SCALAR_ANY));

        RuntimeWiring runtimeWiring = newRuntimeWiring()
                .scalar(GraphQLTimestamp)
                .scalar(GraphQLDate)
                .scalar(GraphQLBase64Binary)
                .scalar(GraphQLTime)
                .scalar(GraphQLAny)
                .type(QUERY_ROOT_TYPE, builder -> builder.defaultDataFetcher(new RootDataFetcher()))
                .type(MUTATION_ROOT_TYPE, builder -> builder.defaultDataFetcher(new RootDataMutator()))
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        int maxNormalFieldComplexity = configuration.getMaxGraphQLQueryComplexity();
        int maxJoinFieldComplexity = configuration.getMaxJoinFieldComplexity();
        int totalMaxGraphQLQueryComplexity = maxNormalFieldComplexity + (maxJoinFieldComplexity * 1000);
        // totalMaxGraphQLQueryComplexity is passed to MaxQueryComplexityInstrumentation but this value is not actually is used
        // as we throw our own exceptions in FieldComplexityCalculatorImpl
        GraphQL build = GraphQL.newGraphQL(graphQLSchema).instrumentation(new MaxQueryComplexityInstrumentation(totalMaxGraphQLQueryComplexity, new FieldComplexityCalculatorImpl(maxJoinFieldComplexity, maxNormalFieldComplexity))).build();
        String id = getServiceIdentifier(tenant, datamodel, datamodelBranch);
        services.put(id, build);
        schemas.put(id, sdl);
    }

    public boolean hasService(String tenant, String datamodel, String datamodelBranch) {
        String id = getServiceIdentifier(tenant, datamodel, datamodelBranch);

        return services.containsKey(id);
    }

    public ExecutionResult execute(String token ,String tenant, String datamodel, String datamodelBranch, String query, ViewType viewType,AccessPrivileges accessPrivileges) {
        return execute(token ,tenant, datamodel, datamodelBranch, query, null, viewType,accessPrivileges);
    }

    public ExecutionResult execute(String token ,String tenant, String datamodel, String datamodelBranch, String query, Map<String, Object> variables, ViewType viewType,AccessPrivileges accessPrivileges) {
        return execute(token ,tenant, datamodel, datamodelBranch, query, variables, viewType, DEFAULT_QUERY_MODEL,accessPrivileges);
    }

    public ExecutionResult execute(String token, String tenant, String datamodel, String datamodelBranch, String query, Map<String, Object> variables, ViewType viewType, RootDataAccess.QueryModel queryModel,AccessPrivileges accessPrivileges) {
        try {
            if (queryModel == null) {
                queryModel = DEFAULT_QUERY_MODEL;
            }
            String serviceIdentifier = getServiceIdentifier(tenant, datamodel, datamodelBranch);
            QueryContext queryContext = new QueryContext(queryModel, tenant, datamodel, datamodelBranch, viewType, variables, token,accessPrivileges);
            ExecutionInput input = new ExecutionInput(query, null, queryContext, null, variables);
            return services.get(serviceIdentifier).execute(input);
        } catch (DataqueryRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DataqueryRuntimeException(e);
        }
    }

    public String getSDL(String tenant, String datamodel, String datamodelBranch) {
    	return schemas.get(getServiceIdentifier(tenant, datamodel, datamodelBranch));
    }

    private String getServiceIdentifier(String tenant, String datamodel, String datamodelBranch) {
        return tenant + "_" + datamodel + "_" + datamodelBranch;
    }
}
