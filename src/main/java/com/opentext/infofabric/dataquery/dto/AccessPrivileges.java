/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.dto;

import com.google.common.collect.Sets;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.graphql.query.FilterSet;

import java.util.Map;
import java.util.Set;

/**
 * Defines the access privilege for the current user request
 *
 * @author open text
 */
public class AccessPrivileges {
    private String tenant;
    private String model;
    private Set<String> readPrivilegesTableSet;
    private Set<String> writePrivilegesTableSet;
    private Map<String, FilterSet> rowSecurityMap;

    public AccessPrivileges() {
    }

    public AccessPrivileges(String tenant, String model, Set<String> readPrivilegesTableSet,
                            Set<String> writePrivilegesTableSet) {
        this.tenant = tenant;
        this.model = model;
        this.readPrivilegesTableSet = readPrivilegesTableSet;
        this.writePrivilegesTableSet = writePrivilegesTableSet;
    }

    public AccessPrivileges(String tenant, String model, Set<String> readPrivilegesTableSet, Set<String> writePrivilegesTableSet, Map<String, FilterSet> rowSecurityMap) {
        this.tenant = tenant;
        this.model = model;
        this.readPrivilegesTableSet = readPrivilegesTableSet;
        this.writePrivilegesTableSet = writePrivilegesTableSet;
        this.rowSecurityMap = rowSecurityMap;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Set<String> getReadPrivilegesTableSet() {
        return readPrivilegesTableSet;
    }

    public void setReadPrivilegesTableSet(Set<String> readPrivilegesTableSet) {
        this.readPrivilegesTableSet = readPrivilegesTableSet;
    }

    public Set<String> getWritePrivilegesTableSet() {
        return writePrivilegesTableSet;
    }

    public void setWritePrivilegesTableSet(Set<String> writePrivilegesTableSet) {
        this.writePrivilegesTableSet = writePrivilegesTableSet;
    }

    public Map<String, FilterSet> getRowSecurityMap() {
        return rowSecurityMap;
    }

    public void setRowSecurityMap(Map<String, FilterSet> rowSecurityMap) {
        this.rowSecurityMap = rowSecurityMap;
    }

    public boolean isAuthorized(Set<String> types, String operation) {
        if (DataqueryConstants.READ_PRIVILEGES.equalsIgnoreCase(operation)) {
            return readPrivilegesTableSet.containsAll(types);
        } else if (DataqueryConstants.WRITE_PRIVILEGES.equalsIgnoreCase(operation)) {
            return writePrivilegesTableSet.containsAll(types);
        } else {
            return false;
        }
    }

    public boolean isAuthorized(String type, String operation) {
        if (DataqueryConstants.READ_PRIVILEGES.equalsIgnoreCase(operation)) {
            return readPrivilegesTableSet.contains(type);
        } else if (DataqueryConstants.WRITE_PRIVILEGES.equalsIgnoreCase(operation)) {
            return writePrivilegesTableSet.contains(type);
        } else {
            return false;
        }
    }

    public Set<String> getUnauthorizedTableNames(Set<String> types, String operation) {
        if (DataqueryConstants.READ_PRIVILEGES.equalsIgnoreCase(operation)) {
            return Sets.difference(types, readPrivilegesTableSet);
        } else if (DataqueryConstants.WRITE_PRIVILEGES.equalsIgnoreCase(operation)) {
            return Sets.difference(types, writePrivilegesTableSet);
        } else {
            return types;
        }
    }

    @Override
    public String toString() {
        return "AccessPrivileges [tenant=" + tenant + ", model=" + model + ", readPrivilegesTableSet="
                + readPrivilegesTableSet + ", writePrivilegesTableSet=" + writePrivilegesTableSet + "]";
    }
}
