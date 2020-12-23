/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.services.impl.sdl;

import com.liaison.datamodel.models.attributes.Attribute;
import com.liaison.datamodel.models.types.Type;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.liaison.dataquery.DataqueryConstants.BY_ID_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.DQ_AGGREGATION_RESULT;
import static com.liaison.dataquery.DataqueryConstants.INPUT_TYPE_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.LINE_BREAK;
import static com.liaison.dataquery.DataqueryConstants.QUERY_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.SCROLL_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.AGGREGATE_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.TAB_BREAK;
import static com.liaison.dataquery.DataqueryConstants.TRIPLE_TAB_BREAK;
import static com.liaison.dataquery.DataqueryConstants.UPSERT_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.INSERT_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.MUTATION_RESULT;


public class SDLRuntimeType {

    private Set<String> attributesAdded = new HashSet<>();

    public enum SDLTypeSignature {
        TYPE, INPUT;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    private final SDLTypeSignature sdlTypeSignature;

    private Type modelType;

    private List<SDLRelationship> sdlRelationshipList;

    private static final Logger log = LoggerFactory.getLogger(SDLRuntimeType.class);

    public SDLRuntimeType(Type modelType, SDLTypeSignature sdlTypeSignature, List<SDLRelationship> sdlRelationshipList) {
        this.modelType = modelType;
        this.sdlTypeSignature = sdlTypeSignature;
        this.sdlRelationshipList = sdlRelationshipList;
    }

    public static SDLRuntimeType instance(Type modelType, SDLTypeSignature sdlType, List<SDLRelationship> sdlRelationshipList) {
        return new SDLRuntimeType(modelType, sdlType, sdlRelationshipList);
    }

    public String typeScroll() {
        return String.format("%1$s %2$s%3$s(skip: Int limit: Int sort: _Sort) : [%2$s]",
                TAB_BREAK, modelType.getName(), SCROLL_METHOD_SUFFIX);
    }

    public String typeById() {
        return String.format("%1$s %2$s%3$s(id: ID!) : %2$s",
                TAB_BREAK, modelType.getName(), BY_ID_METHOD_SUFFIX);
    }

    public String typeQuery() {
        return String.format("%1$s %2$s%3$s(filterset: _FilterSet! skip: Int limit: Int sort: _Sort) : [%2$s]",
                TAB_BREAK, modelType.getName(), QUERY_METHOD_SUFFIX);
    }

    public String typeAggregate() {
        return String.format("%1$s %2$s%3$s(filterset: _FilterSet sort: _Sort aggregate: _Aggregate!) : [%2$s]",
                TAB_BREAK, modelType.getName(), AGGREGATE_METHOD_SUFFIX);
    }

    public String typeUpsert() {
        return String.format("%1$s %2$s%3$s(input: [%2$s%4$s]!) : %5$s",
                TAB_BREAK, modelType.getName(), UPSERT_METHOD_SUFFIX, INPUT_TYPE_SUFFIX, MUTATION_RESULT);
    }

    public String typeInsert() {
        return String.format("%1$s %2$s%3$s(input: [%2$s%4$s]!) : %5$s",
                TAB_BREAK, modelType.getName(), INSERT_METHOD_SUFFIX, INPUT_TYPE_SUFFIX, MUTATION_RESULT);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(idField());
        if (sdlRelationshipList != null && !sdlRelationshipList.isEmpty()) {
            this.sdlRelationshipList.stream().forEach(sdlRelationship -> {
                if (!attributesAdded.contains(sdlRelationship.getParentKeyFieldName())) {
                    builder.append(getForeignKeyAttribute(sdlRelationship.getParentKeyFieldName()));
                    attributesAdded.add(sdlRelationship.getParentKeyFieldName());
                }
            });
        }
        if (modelType.getAttributes() != null) {
            modelType.getAttributes().stream().forEach(attr -> {
                if (!attributesAdded.contains(attr.toString())) {
                    builder.append(SDLField.instance(attr).toString()).append(LINE_BREAK);
                    attributesAdded.add(attr.toString());
                }
            });
        } else {
            log.warn("model type with id : {} and name : {} does not have attributes", modelType.getId(), modelType.getName());
        }
        String typeName = modelType.getName();
        if (sdlTypeSignature.equals(SDLTypeSignature.TYPE)) {
            builder.append(aggregateField());
        } else {
            typeName += INPUT_TYPE_SUFFIX;
        }
        if (sdlRelationshipList != null && !sdlRelationshipList.isEmpty()) {
            this.sdlRelationshipList.stream().forEach(sdlRelationship -> {
                builder.append(getRelationship(sdlRelationship));
            });
        }
        return String.format("%s %s { %s %s }", sdlTypeSignature.toString(), typeName, LINE_BREAK, builder.toString());
    }

    public String getInputTypes() {
        StringBuilder builder = new StringBuilder();
        builder.append(idFieldForInput());
        List<Attribute> attrList = modelType.getAttributes();
        ArrayList<Attribute> equalityList = null;

        if (modelType.getEquality() != null && modelType.getEquality().getFields() != null) {
            equalityList = modelType.getEquality().getFields();
            attrList.removeAll(equalityList);
        }

        if (attrList != null) {
            attrList.forEach(attr -> {
                if (!attributesAdded.contains(attr.toString())) {
                    {
                        builder.append(SDLField.instance(attr).getAttribute()).append(LINE_BREAK);
                        attributesAdded.add(attr.toString());
                    }
                }
            });
        }
        if (equalityList != null) {
            equalityList.forEach(eqattr -> {
                if (!attributesAdded.contains(eqattr.toString())) {
                    {
                        builder.append(SDLField.instance(eqattr).getEqualityKeys()).append(LINE_BREAK);
                        attributesAdded.add(eqattr.toString());
                    }
                }
            });
        }

        String typeName = modelType.getName();
        typeName += INPUT_TYPE_SUFFIX;

        if (sdlRelationshipList != null && !sdlRelationshipList.isEmpty()) {
            this.sdlRelationshipList.stream().forEach(sdlRelationship -> {
                builder.append(getRelationshipForInput(sdlRelationship));
            });
        }
        return String.format("%s %s { %s %s }", sdlTypeSignature.toString(), typeName, LINE_BREAK, builder.toString());
    }

    private String idField() {
        if (modelType.getKeyFieldName() != null && !modelType.getKeyFieldName().equalsIgnoreCase("null")) {
            attributesAdded.add(modelType.getKeyFieldName());
            return String.format("%s%s: ID!%s", TAB_BREAK, modelType.getKeyFieldName(), LINE_BREAK);
        }
        attributesAdded.add("id");
        return String.format("%s%s: ID! %s %s", TAB_BREAK, "id", "@compilerDefault(class: \"PK\")", LINE_BREAK);
    }

    private String idFieldForInput() {
        if (modelType.getKeyFieldName() != null && !modelType.getKeyFieldName().equalsIgnoreCase("null")) {
            attributesAdded.add(modelType.getKeyFieldName());
            return String.format("%s%s: ID%s", TAB_BREAK, modelType.getKeyFieldName(), LINE_BREAK);
        }
        attributesAdded.add("id");
        return String.format("%s%s: ID %s %s", TAB_BREAK, "id", "@compilerDefault(class: \"PK\")", LINE_BREAK);
    }

    private String getForeignKeyAttribute(String foreignKey) {
        return String.format("%s%s: ID %s %s", TAB_BREAK, foreignKey, "@compilerDefault(class: \"FK\")", LINE_BREAK);
    }

    private String aggregateField() {
        return String.format("%s%s: Float%s", TAB_BREAK, DQ_AGGREGATION_RESULT, LINE_BREAK);
    }

    private String getRelationship(SDLRelationship sdlRelationship) {
        String filterSetStr = "";
        String childType = sdlRelationship.getChildType();
        if (sdlRelationship.getChildCardinality() == -1) {
            filterSetStr = "(filterset:_FilterSet)";
            childType = "[" + childType + "]";
        }
        if (sdlRelationship.getBridgeName() != null) {
            return String.format("%s%s%s: %s @relationship(parentKeyFieldName: \"%s\"%s%s childKeyFieldName: \"%s\" " +
                            "%s%s bridgeName: \"%s\" %s%s parentBridgeKeyFieldName: \"%s\"" +
                            "%s%s childBridgeKeyFieldName: \"%s\")%s", TAB_BREAK,
                    sdlRelationship.getRelationshipName(), filterSetStr, childType,
                    sdlRelationship.getParentKeyFieldName(), LINE_BREAK, TRIPLE_TAB_BREAK,
                    sdlRelationship.getChildKeyFieldName(), LINE_BREAK, TRIPLE_TAB_BREAK,
                    sdlRelationship.getBridgeName(), LINE_BREAK, TRIPLE_TAB_BREAK,
                    sdlRelationship.getParentBridgeKeyFieldName(), LINE_BREAK, TRIPLE_TAB_BREAK,
                    sdlRelationship.getChildBridgeKeyFieldName(), LINE_BREAK);
        } else {
            return String.format("%s%s%s: %s @relationship(parentKeyFieldName: \"%s\"%s%s childKeyFieldName: \"%s\")%s", TAB_BREAK,
                    sdlRelationship.getRelationshipName(), filterSetStr, childType,
                    sdlRelationship.getParentKeyFieldName(), LINE_BREAK, TRIPLE_TAB_BREAK,
                    sdlRelationship.getChildKeyFieldName(), LINE_BREAK);
        }
    }

    private String getRelationshipForInput(SDLRelationship sdlRelationship) {
        String childType = sdlRelationship.getChildType();
        childType += INPUT_TYPE_SUFFIX;
        if (sdlRelationship.getChildCardinality() == -1) {
            childType = "[" + childType + "]";
        }
        return String.format("%s%s: %s%s", TAB_BREAK, sdlRelationship.getRelationshipName(), childType, LINE_BREAK);

    }

}