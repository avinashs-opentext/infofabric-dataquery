/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.query;

import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.graphql.RootDataAccess;
import com.liaison.dataquery.graphql.helpers.TypeMapper;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JoinCondition {
    static final String PARENT_FIELD = "parentKeyFieldName";
    static final String CHILD_FIELD = "childKeyFieldName";
    private static final String BRIDGE_NAME = "bridgeName";
    private static final String BRIDGE_PARENT_FIELD = "parentBridgeKeyFieldName";
    private static final String BRIDGE_CHILD_FIELD = "childBridgeKeyFieldName";

    private Object parentTypeId;
    private String parentIdField;
    private String parentIdFieldOnChild;
    private boolean hasMany;
    private String bridgeName;
    private String parentBridgeKeyFieldName;
    private String childBridgeKeyFieldName;
    private FilterSet filterSet;
    private List<String> path = new ArrayList<>();
    private List<JoinCondition> children = new ArrayList<>();
    private String targetType;
    private GraphQLType targetObjectType;

    public JoinCondition(Object parentTypeId, String parentIdField, String parentIdFieldOnChild, boolean hasMany) {
        this.parentTypeId = parentTypeId;
        this.parentIdField = parentIdField;
        this.parentIdFieldOnChild = parentIdFieldOnChild;
        this.hasMany = hasMany;
    }

    public JoinCondition(Object parentTypeId, String parentIdField, String parentIdFieldOnChild, boolean hasMany,
                         String bridgeName, String parentBridgeKeyFieldName, String childBridgeKeyFieldName) {
        this.parentTypeId = parentTypeId;
        this.parentIdField = parentIdField;
        this.parentIdFieldOnChild = parentIdFieldOnChild;
        this.hasMany = hasMany;
        this.bridgeName = bridgeName;
        this.parentBridgeKeyFieldName = parentBridgeKeyFieldName;
        this.childBridgeKeyFieldName = childBridgeKeyFieldName;
    }

    public Object getParentTypeId() {
        return parentTypeId;
    }

    public void setParentTypeId(Object parentTypeId) {
        this.parentTypeId = parentTypeId;
    }

    public String getParentIdFieldOnChild() {
        return parentIdFieldOnChild;
    }

    public void setParentIdFieldOnChild(String parentIdFieldOnChild) {
        this.parentIdFieldOnChild = parentIdFieldOnChild;
    }

    public String getParentIdField() {
        return parentIdField;
    }

    public void setParentIdField(String parentIdField) {
        this.parentIdField = parentIdField;
    }

    public String getBridgeName() {
        return bridgeName;
    }

    public void setBridgeName(String bridgeName) {
        this.bridgeName = bridgeName;
    }

    public String getParentBridgeKeyFieldName() {
        return parentBridgeKeyFieldName;
    }

    public void setParentBridgeKeyFieldName(String parentBridgeKeyFieldName) {
        this.parentBridgeKeyFieldName = parentBridgeKeyFieldName;
    }

    public String getChildBridgeKeyFieldName() {
        return childBridgeKeyFieldName;
    }

    public void setChildBridgeKeyFieldName(String childBridgeKeyFieldName) {
        this.childBridgeKeyFieldName = childBridgeKeyFieldName;
    }

    public static JoinCondition build(RootDataAccess.QueryModel queryModel, GraphQLFieldDefinition fieldSDL,
                                      Map<String, Object> parentResult) {
        GraphQLDirective directive = fieldSDL.getDirective(DataqueryConstants.RELATIONSHIP_KEY);
        if (directive == null) {
            throw new DataqueryRuntimeException("Nested object missing @relationship directive from the GraphQL schema.");
        }
        String parentField = (String) directive.getArgument(PARENT_FIELD).getValue();
        if (parentField == null) {
            throw new DataqueryRuntimeException("@relationship directive missing children field name from the GraphQL schema.");
        }
        String childField = (String) directive.getArgument(CHILD_FIELD).getValue();
        if (childField == null) {
            throw new DataqueryRuntimeException("@relationship directive missing child field name from the GraphQL schema.");
        }

        Object parentId = null;
        if (queryModel.equals(RootDataAccess.QueryModel.MULTI_QUERY)) {
            parentId = parentResult.get(parentField);
            if (parentId == null) {
                return null;
            }
        }

        boolean hasMany = TypeMapper.getNonNullType(fieldSDL.getType()) instanceof GraphQLList;

        String bridgeName = (String) directive.getArgument(BRIDGE_NAME).getValue();

        if (!StringUtils.isBlank(bridgeName)) {
            String parentBridgeField = (String) directive.getArgument(BRIDGE_PARENT_FIELD).getValue();
            String childBridgeField = (String) directive.getArgument(BRIDGE_CHILD_FIELD).getValue();
            return new JoinCondition(parentId, parentField, childField, hasMany,
                    bridgeName, parentBridgeField, childBridgeField);
        }

        return new JoinCondition(parentId, parentField, childField, hasMany);
    }

    public boolean hasMany() {
        return hasMany;
    }

    public FilterSet getFilterSet() {
        return filterSet;
    }

    public void setFilterSet(FilterSet filterSet) {
        this.filterSet = filterSet;
    }

    public void setPath(List<String> currentPath) {
        this.path = currentPath;
    }

    public void addChild(JoinCondition parent) {
        this.children.add(parent);
    }

    public List<String> getPath() {
        return path;
    }

    public List<JoinCondition> getChildren() {
        return children;
    }

    public void setTargetType(String type) {
        this.targetType = type;
    }

    public String getTargetType() {
        return targetType;
    }

    public GraphQLType getTargetObjectType() {
        return targetObjectType;
    }

    public void setTargetObjectType(GraphQLType targetObjectType) {
        this.targetObjectType = targetObjectType;
    }
}
