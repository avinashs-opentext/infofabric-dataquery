/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.query;

import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.RootDataAccess;
import com.opentext.infofabric.dataquery.graphql.helpers.TypeMapper;
import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectionSet {

    private ArrayList<ProjectionSet> children;
    private List<String> path;


    public String getIdField() {
        return idField;
    }

    public void setIdField(String idField) {
        this.idField = idField;
    }

    private String idField;

    public ProjectionSet() {
    }

    private final Set<Projection> projections = new HashSet<>();

    public void addProjection(String field, String alias, String dataType) {
        addProjection(field, alias, dataType, false);
    }


    private void addProjection(String field, String alias, String dataType, boolean autoAdd) {
        Projection projection = new Projection(field, alias, dataType, autoAdd);
        if (!projections.contains(projection)) {
            projections.add(projection);
        }
    }

    public Set<Projection> getProjections() {
        return projections;
    }


    public static ProjectionSet buildAttributeProjection(RootDataAccess.QueryModel queryModel, GraphQLObjectType currentType,
                                                         SelectionSet selections) {
        ProjectionSet projectionSet = new ProjectionSet();
        // Build projection set: 1. Add all selected scalar attributes, 2. Add all keys that may be used in future joins
        boolean hasId = false;
        for (Selection selection : selections.getSelections()) {
            Field f = (Field) selection;
            GraphQLFieldDefinition fieldSDL = currentType.getFieldDefinition(f.getName());
            GraphQLType dataType = TypeMapper.getNonNullType(fieldSDL.getType());

            if (dataType instanceof GraphQLScalarType) {
                projectionSet.addProjection(f.getName(), f.getAlias(), dataType.getName());
                if (TypeMapper.getNonNullType(dataType).getName().equals("ID") && (fieldSDL.getType() instanceof GraphQLNonNull)) {
                    hasId = true;
                    projectionSet.setIdField(f.getName());
                }
            } else if (fieldSDL.getDirective(DataqueryConstants.RELATIONSHIP_KEY) != null) {
                addKeyFieldProjections(fieldSDL, currentType, projectionSet);
            } else {
                throw new DataqueryRuntimeException("Invalid schema. Field has to be either a Scalar Type or contain a @relationship directive.");
            }
        }

        // Projections need to have primary key included
        if (!hasId) {
            currentType.getFieldDefinitions().forEach(fdef -> {
                GraphQLType outputType = TypeMapper.getNonNullType(fdef.getType());
                if ("ID".equals(outputType.getName()) && (fdef.getType() instanceof GraphQLNonNull)) {
                    projectionSet.addProjection(fdef.getName(), null, outputType.getName(), true);
                    projectionSet.setIdField(fdef.getName());
                }
            });
        }

        return projectionSet;
    }


    private static void addKeyFieldProjections(GraphQLFieldDefinition fieldSDL, GraphQLObjectType currentType,
                                               ProjectionSet projectionSet) {
        GraphQLDirective relationship = fieldSDL.getDirective("relationship");

        // Key columns are part of the SDL and not the query
        String parentKeyFieldName = (String) relationship.getArgument(JoinCondition.PARENT_FIELD).getValue();

        // Parent key field may be db specific and not part of the SDL (eg. _key in Arango, special row key in HBase)
        // If not part of the SDL let DB specific implementation figure it out.
        String parentKeyFieldType = null;
        if (currentType.getFieldDefinition(parentKeyFieldName) != null) {
            GraphQLType type = currentType.getFieldDefinition(parentKeyFieldName).getType();
            parentKeyFieldType = TypeMapper.getNonNullType(type).getName();
        }
        projectionSet.addProjection(parentKeyFieldName, null, parentKeyFieldType, true);
    }

    @Override
    public String toString() {
        return projections.toString();
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public void addChild(ProjectionSet child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }

    public ArrayList<ProjectionSet> getChildren() {
        return children;
    }

    public List<String> getPath() {
        return path;
    }
}
