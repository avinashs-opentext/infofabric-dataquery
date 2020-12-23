/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.dto;

public class NamedQueryVariable {
    private String name;
    private String type;
    private String value;


    @Override
    public String toString() {
        return "NamedQueryVariable{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NamedQueryVariable){
            NamedQueryVariable nqv = (NamedQueryVariable)obj;
            return nqv.getName().equalsIgnoreCase(this.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public void setValue(String value) {
        this.value = value;
    }
}
