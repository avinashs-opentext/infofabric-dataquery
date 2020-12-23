/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.traversal;

import com.liaison.dataquery.exception.DataqueryException;
import com.liaison.dataquery.graphql.query.QueryContext;
import com.liaison.dataquery.graphql.results.ResultList;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLType;

import java.util.List;

public interface DataQueryTraversal {
    ResultList execute(QueryContext queryContext, GraphQLType rootType, Field rootField, SelectionSet rootSelections, List<Argument> rootArguments) throws DataqueryException;
}
