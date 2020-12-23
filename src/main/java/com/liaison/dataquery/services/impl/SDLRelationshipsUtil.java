/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.services.impl;

import com.liaison.datamodel.models.Model;
import com.liaison.datamodel.models.relationships.Relationship;
import com.liaison.datamodel.models.types.Type;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.services.impl.sdl.SDLRelationship;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by vkukkadapu on 10/26/18.
 */
public class SDLRelationshipsUtil {

    private static final Logger log = LoggerFactory.getLogger(SDLRelationshipsUtil.class);


    public void populateMember(Model model, Map<String, List<SDLRelationship>> sdlRelationshipMap, Relationship relationship,
                               int idx0, int idx1) {
        List<SDLRelationship> sdlRelationshipList = new ArrayList<>();
        SDLRelationship sdlRelationship = new SDLRelationship();
        sdlRelationship.setRelationshipName(relationship.getMembers().get(idx0).getName());
        sdlRelationship.setChildType(getSpecificType(model.getTypes(), relationship.getMembers().get(idx1).getTypeId()).getName());
        sdlRelationship.setParentCardinality(relationship.getMembers().get(idx0).getCardinality().getMax());
        sdlRelationship.setChildCardinality(relationship.getMembers().get(idx1).getCardinality().getMax());
        setParentAndChildKeyFields(model, relationship, sdlRelationship, idx0, idx1);
        if (sdlRelationshipMap.containsKey(relationship.getMembers().get(idx0).getTypeId())) {
            sdlRelationshipList = sdlRelationshipMap.get(relationship.getMembers().get(idx0).getTypeId());
        }
        sdlRelationshipList.add(sdlRelationship);
        sdlRelationshipMap.put(relationship.getMembers().get(idx0).getTypeId(), sdlRelationshipList);
    }

    private void setParentAndChildKeyFields(Model model, Relationship relationship, SDLRelationship sdlRelationship, int idx0, int idx1) {
        if ((sdlRelationship.getParentCardinality() == 1) && (sdlRelationship.getChildCardinality() == -1)) {
            handleOneToManyRelationship(model, relationship, sdlRelationship, idx0, idx1);
        } else if ((sdlRelationship.getParentCardinality() == -1) && (sdlRelationship.getChildCardinality() == 1)) {
            handleManyToOneRelationship(model, relationship, sdlRelationship, idx0, idx1);
        } else if ((sdlRelationship.getParentCardinality() == 1) && (sdlRelationship.getChildCardinality() == 1)) {
            handleOneToOneRelationship(model, relationship, sdlRelationship, idx0, idx1);
        } else if ((sdlRelationship.getParentCardinality() == -1) && (sdlRelationship.getChildCardinality() == -1)) {
            handleManyToManyRelationship(model, relationship, sdlRelationship, idx0, idx1);
        }
    }

    private void handleManyToManyRelationship(Model model, Relationship relationship, SDLRelationship sdlRelationship, int idx0, int idx1) {
        // set parentKeyFieldName
        Type parentType = getSpecificType(model.getTypes(), relationship.getMembers().get(idx0).getTypeId());
        String parentKeyFieldName = parentType.getKeyFieldName();
        if (StringUtils.isBlank(parentKeyFieldName)) {
            parentKeyFieldName = "id";
        }
        sdlRelationship.setParentKeyFieldName(parentKeyFieldName);
        // set childKeyFieldName
        Type childType = getSpecificType(model.getTypes(), relationship.getMembers().get(idx1).getTypeId());
        String childKeyFieldName = childType.getKeyFieldName();
        if (StringUtils.isBlank(childKeyFieldName)) {
            childKeyFieldName = "id";
        }
        sdlRelationship.setChildKeyFieldName(childKeyFieldName);
        setBridgeNameAndBridgeKeyFields(relationship, sdlRelationship, idx0, idx1, parentType, childType);
    }

    private void setBridgeNameAndBridgeKeyFields(Relationship relationship, SDLRelationship sdlRelationship, int idx0, int idx1, Type parentType, Type childType) {
        // Generate and set BridgeName and parent, child BridgeKeyFieldNames based on logic in DataModel
        String parentBridgeKeyFieldName = parentType.getName() + "_" + relationship.getMembers().get(idx0).getName() + "_id";
        sdlRelationship.setParentBridgeKeyFieldName(parentBridgeKeyFieldName);
        String childBridgeKeyFieldName = childType.getName() + "_" + relationship.getMembers().get(idx1).getName() + "_id";
        sdlRelationship.setChildBridgeKeyFieldName(childBridgeKeyFieldName);
        String bridgeName = getBridgeTableName(relationship, idx0, parentType, childType);
        sdlRelationship.setBridgeName(bridgeName);
    }

    public String getBridgeTableName(Relationship relationship, int idx0, Type parentType, Type childType) {
        List<String> typeNames = new ArrayList();
        typeNames.add(parentType.getName());
        typeNames.add(childType.getName());
        // sort the types in ASC order
        Collections.sort(typeNames, String::compareToIgnoreCase);
        return typeNames.get(0) + "_" + relationship.getMembers().get(idx0).getName() + "_" + typeNames.get(1);
    }

    private void handleOneToOneRelationship(Model model, Relationship relationship, SDLRelationship sdlRelationship, int idx0, int idx1) {
        sdlRelationship.setParentKeyFieldName(relationship.getMembers().get(idx0).getKeyFieldName());
        Type childType = getSpecificType(model.getTypes(), relationship.getMembers().get(idx1).getTypeId());
        String childKeyFieldName = childType.getKeyFieldName();
        if (StringUtils.isBlank(childKeyFieldName)) {
            childKeyFieldName = "id";
        }
        sdlRelationship.setChildKeyFieldName(childKeyFieldName);
    }

    private void handleManyToOneRelationship(Model model, Relationship relationship, SDLRelationship sdlRelationship, int idx0, int idx1) {
        Type childType = getSpecificType(model.getTypes(), relationship.getMembers().get(idx1).getTypeId());
        String childKeyFieldName = childType.getKeyFieldName();
        if (StringUtils.isBlank(childKeyFieldName)) {
            childKeyFieldName = "id";
        }
        sdlRelationship.setChildKeyFieldName(childKeyFieldName);
        String parentKeyFieldName = relationship.getMembers().get(idx0).getKeyFieldName();
        if (StringUtils.isBlank(parentKeyFieldName)) {
            // membername_othertypename
            // See default-fk-name in https://github.com/LiaisonTechnologies/dm-datamodel/blob/master/client/src/clj/datamodel/compiler/rdbms.clj
            parentKeyFieldName = relationship.getMembers().get(idx0).getName() + "_" + childType.getName() + "_id";
        }
        sdlRelationship.setParentKeyFieldName(parentKeyFieldName);
    }

    private void handleOneToManyRelationship(Model model, Relationship relationship, SDLRelationship sdlRelationship, int idx0, int idx1) {
        Type parentType = getSpecificType(model.getTypes(), relationship.getMembers().get(idx0).getTypeId());
        String parentKeyFieldName = parentType.getKeyFieldName();
        if (StringUtils.isBlank(parentKeyFieldName)) {
            parentKeyFieldName = "id";
        }
        sdlRelationship.setParentKeyFieldName(parentKeyFieldName);
        String childKeyFieldName = relationship.getMembers().get(idx1).getKeyFieldName();
        if (StringUtils.isBlank(childKeyFieldName)) {
            // membername_othertypename
            // See default-fk-name in https://github.com/LiaisonTechnologies/dm-datamodel/blob/master/client/src/clj/datamodel/compiler/rdbms.clj
            childKeyFieldName = relationship.getMembers().get(idx1).getName() + "_" + parentType.getName() + "_id";
        }
        sdlRelationship.setChildKeyFieldName(childKeyFieldName);
    }

    public Type getSpecificType(List<Type> types, String typeId) {
        for (Type t : types) {
            if (t.getId().equals(typeId)) {
                return t;
            }
        }
        log.error("Type is null for typeId {}", typeId);
        throw new DataqueryRuntimeException(String.format("Type is null for the typeId { %s }", typeId));
    }

}
