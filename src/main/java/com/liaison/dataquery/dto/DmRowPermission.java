/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.dto;

import java.util.List;

/**
 * This class represents the root row_level security object.
 *
 * @author open text
 */
public class DmRowPermission  {
    public DmRowPermission(List<DmFilterset> filtersets) {
        this.filtersets = filtersets;
    }

    public DmRowPermission(String typeName, List<DmFilterset> filtersets) {
        this.typeName = typeName;
        this.filtersets = filtersets;
    }

    public DmRowPermission() {
    }

    private String typeName;

    private List<DmFilterset> filtersets;

    public List<DmFilterset> getFiltersets() {
        return filtersets;
    }

    public void setFiltersets(List<DmFilterset> filtersets) {
        this.filtersets = filtersets;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return "DmRowPermission{" +
                "typeName='" + typeName + '\'' +
                ", filtersets=" + filtersets +
                '}';
    }
}
