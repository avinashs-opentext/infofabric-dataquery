/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.traversal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.liaison.dataquery.graphql.query.Filter;
import io.jsonwebtoken.lang.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.exception.DataqueryException;
import com.liaison.dataquery.graphql.RootDataAccess;
import com.liaison.dataquery.graphql.dataloaders.DataQuerySimpleLoader;
import com.liaison.dataquery.graphql.dataloaders.QueryLoaderFactory;
import com.liaison.dataquery.graphql.helpers.TypeMapper;
import com.liaison.dataquery.graphql.query.FilterSet;
import com.liaison.dataquery.graphql.query.JoinCondition;
import com.liaison.dataquery.graphql.query.ProjectionSet;
import com.liaison.dataquery.graphql.query.Query;
import com.liaison.dataquery.graphql.query.QueryContext;
import com.liaison.dataquery.graphql.results.ResultList;

import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

public class SingleTraversal implements DataQueryTraversal {

    private static final Logger logger = LoggerFactory.getLogger(SingleTraversal.class);

    @Override
    public ResultList execute(QueryContext queryContext, GraphQLType rootType, Field rootField, SelectionSet rootSelections, List<Argument> rootArguments) throws DataqueryException {
		// Root query contains first level Filters and Projections
        Query rootQuery = Query.build((GraphQLObjectType) rootType, rootSelections, rootArguments, queryContext);
		Set<String> tableSet = new HashSet<String>();
        // Recursively parse selections and build on the root query
        // All nested objects will create a join condition
        // All filters from now on will he handled as join filters
        // All selected fields will be added to the root projections with object path included
        GraphQLObjectType currentType = (GraphQLObjectType) rootType;
        tableSet.add(currentType.getName());
        List<String> path = new ArrayList<>();
        path.add(currentType.getName());
        rootQuery.getProjectionSet().setPath(path);
        traverse(rootQuery, currentType, rootSelections, path, null, rootQuery.getProjectionSet(),tableSet);

        DataQuerySimpleLoader simpleLoader = QueryLoaderFactory.getSimpleLoader(queryContext.getViewType());
		if (logger.isDebugEnabled()) {
			logger.debug("Tables included in query" + tableSet.toString());
		}

		if (queryContext.getAccessPrivileges().isAuthorized(tableSet, DataqueryConstants.READ_PRIVILEGES)) {
            FilterSet queryFilterSet = rootQuery.getFilterSet();
            rootQuery.setFilterSet( populateFilterSetForType(rootQuery.getCollection().getName(), rootType, queryFilterSet, queryContext) );
            if(! Collections.isEmpty( rootQuery.getJoinConditions() ) ) {
                for (JoinCondition jc: rootQuery.getJoinConditions()) {
                    populateJoinConditionFiltersets(jc, queryContext);
                }
            }
			return simpleLoader.load(rootQuery);
		} else {
			throw new DataqueryException("User is not authorized to access " + queryContext.getAccessPrivileges()
					.getUnauthorizedTableNames(tableSet, DataqueryConstants.READ_PRIVILEGES));
		}
    }

    private void populateJoinConditionFiltersets(JoinCondition jc,QueryContext queryContext) {
        jc.setFilterSet( populateFilterSetForType(jc.getTargetType(), jc.getTargetObjectType(), jc.getFilterSet(), queryContext) );
        if ( ! Collections.isEmpty( jc.getChildren() )) {
            for (JoinCondition childCondition: jc.getChildren() ) {
                populateJoinConditionFiltersets( childCondition, queryContext);
            }
        }
    }

    private FilterSet populateFilterSetForType(String type, GraphQLType currentType, FilterSet queryFilterSet, QueryContext queryContext) {
        FilterSet rootFilterSet = queryContext.getAccessPrivileges().getRowSecurityMap().get(type);
        if(null != rootFilterSet && !Collections.isEmpty(rootFilterSet.getFiltersets())) {
            for (FilterSet filterset : rootFilterSet.getFiltersets()) {
                for (Filter filter : filterset.getFilters()) {
                    filter.setFieldType(FilterSet.getFieldTypeOfGraphQLField((GraphQLObjectType) currentType, filter.getFieldName()));
                }
            }
        }
        FilterSet newFilterSet = new FilterSet();
        newFilterSet.initFiltersets();
        if(null != rootFilterSet )
            newFilterSet.getFiltersets().add(rootFilterSet);
        if(null != queryFilterSet)
            newFilterSet.getFiltersets().add(queryFilterSet);
        newFilterSet.setLogicalOperator(FilterSet.LogicalOperator.AND);
        return !Collections.isEmpty(newFilterSet.getFiltersets()) ?  newFilterSet : null;
    }

    private void traverse(Query rootQuery, GraphQLObjectType previousType, SelectionSet currentSelection,
                          List<String> path, JoinCondition parentJoin, ProjectionSet parentProjection,Set<String> tableSet) {
        if (rootQuery == null || currentSelection == null || previousType == null) {
            return;
        }
        for (Selection selection : currentSelection.getSelections()) {
            List<String> currentPath = new ArrayList<>(path);
            Field selectedField = (Field) selection;
            GraphQLFieldDefinition fieldSDL = previousType.getFieldDefinition(selectedField.getName());
			if (fieldSDL.getDirective(DataqueryConstants.RELATIONSHIP_KEY) != null) {
				GraphQLObjectType currentType = (GraphQLObjectType) TypeMapper.getWrappedType(fieldSDL.getType());
				tableSet.add(currentType.getName());
				currentPath.add(fieldSDL.getName());

                ProjectionSet projectionSet = ProjectionSet.buildAttributeProjection(
                        RootDataAccess.QueryModel.SINGLE_QUERY,
                        currentType,
                        selectedField.getSelectionSet());
                projectionSet.setPath(currentPath);
                parentProjection.addChild(projectionSet);

                FilterSet joinFilters = FilterSet.buildQueryFilter(selectedField.getArguments(), currentType);

                JoinCondition joinCondition = JoinCondition.build(RootDataAccess.QueryModel.SINGLE_QUERY, fieldSDL, null);
                joinCondition.setFilterSet(joinFilters);
                joinCondition.setPath(currentPath);
                joinCondition.setTargetType(TypeMapper.getWrappedType(fieldSDL.getType()).getName());
                joinCondition.setTargetObjectType(currentType);
                // The join starts at root level, add to root query
                if (parentJoin == null) {
                    rootQuery.addJoinCondition(joinCondition);
                } else {
                    parentJoin.addChild(joinCondition);
                }

                traverse(rootQuery, currentType, selectedField.getSelectionSet(), currentPath, joinCondition, projectionSet,tableSet);
            }
        }
    }
}
