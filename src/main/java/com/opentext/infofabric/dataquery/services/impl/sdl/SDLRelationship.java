/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.services.impl.sdl;

/**
 * Created by vkukkadapu on 10/22/18.
 */
public class SDLRelationship {

    private String relationshipName;
    private String childType;
    private int parentCardinality;
    private int childCardinality;
    private String parentKeyFieldName;
    private String childKeyFieldName;
    private String bridgeName;

    private String parentBridgeKeyFieldName;
    private String childBridgeKeyFieldName;

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public String getChildType() {
        return childType;
    }

    public void setChildType(String childType) {
        this.childType = childType;
    }

    public int getChildCardinality() {
        return childCardinality;
    }

    public void setChildCardinality(int childCardinality) {
        this.childCardinality = childCardinality;
    }

    public int getParentCardinality() {
        return parentCardinality;
    }

    public void setParentCardinality(int parentCardinality) {
        this.parentCardinality = parentCardinality;
    }

    public String getParentKeyFieldName() {
        return parentKeyFieldName;
    }

    public void setParentKeyFieldName(String parentKeyFieldName) {
        this.parentKeyFieldName = parentKeyFieldName;
    }

    public String getChildKeyFieldName() {
        return childKeyFieldName;
    }

    public void setChildKeyFieldName(String childKeyFieldName) {
        this.childKeyFieldName = childKeyFieldName;
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

}
