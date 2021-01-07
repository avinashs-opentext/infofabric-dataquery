/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql;

import com.opentext.infofabric.dataquery.exception.DataqueryException;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.helpers.TypeMapper;
import com.opentext.infofabric.dataquery.graphql.query.QueryContext;
import com.opentext.infofabric.dataquery.graphql.results.ResultList;
import com.opentext.infofabric.dataquery.graphql.traversal.DataQueryTraversal;
import com.opentext.infofabric.dataquery.graphql.traversal.RecursiveTraversal;
import com.opentext.infofabric.dataquery.graphql.traversal.SingleTraversal;
import graphql.execution.DataFetcherResult;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RootDataFetcher implements RootDataAccess {

    private final Logger logger = LoggerFactory.getLogger(RootDataFetcher.class);

    private final DataQueryTraversal recursiveQueryTraversal;
    private final DataQueryTraversal singleQueryTraversal;


    RootDataFetcher() {
        this.recursiveQueryTraversal = new RecursiveTraversal();
        this.singleQueryTraversal = new SingleTraversal();
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        // root field is the query method name
        Field rootField = environment.getField();
        QueryContext queryContext = environment.getContext();
        queryContext.setQueryType(RootDataAccess.resolveQueryType(rootField.getName()));
        List<Argument> rootArguments = rootField.getArguments();
        TypeMapper.bindVariables(rootArguments, queryContext.getVariables(), environment.getFieldDefinition());
        SelectionSet rootSelections = rootField.getSelectionSet();

        //Expected return type. Also the type of the root.
        GraphQLType rootType = environment.getFieldType();
        if (rootType instanceof GraphQLList) {
            rootType = ((GraphQLList) rootType).getWrappedType();
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Executing query %s with projections %s and filters %s expecting return type %s",
                    rootField.getName(), rootSelections, rootArguments, rootType.getName()));
        }

        DataQueryTraversal traverser = recursiveQueryTraversal;
        if (queryContext.getQueryModel().equals(QueryModel.SINGLE_QUERY)) {
            traverser = singleQueryTraversal;
        }

        ResultList results;
		try {
			results = traverser.execute(queryContext, rootType, rootField, rootSelections, rootArguments);
			 if (logger.isTraceEnabled()) {
		            logger.trace(String.format("Execution result %s", results));
		        }

		        if (queryContext.getQueryType().equals(QueryType.BY_ID)) {
		            if (!results.isEmpty()) {
		                return new DataFetcherResult<>(results.get(0), new ArrayList<>());
		            }
		            return new DataFetcherResult<>(null, new ArrayList<>());
		        }
		        return new DataFetcherResult<>(results, new ArrayList<>());
		} catch (DataqueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DataqueryRuntimeException(e);
		}

       
    }

}
