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
 * This class represents list of read, write and row_security privilege for the given model and tenant
 * based on configuration store entries in java application cache.
 *
 * @author open text
 */
public class DmPrivileges {

    private List<String> read_privileges;

    private List<String> write_privileges;

    private List<DmRowPermission> row_security;

    public DmPrivileges() {
    }

    public DmPrivileges(List<String> read_privileges, List<String> write_privileges) {
        this.read_privileges = read_privileges;
        this.write_privileges = write_privileges;
    }

    public DmPrivileges(List<String> read_privileges, List<String> write_privileges, List<DmRowPermission> row_security) {
        this.read_privileges = read_privileges;
        this.write_privileges = write_privileges;
        this.row_security = row_security;
    }

    public List<String> getRead_privileges() {
        return read_privileges;
    }

    public void setRead_privileges(List<String> read_privileges) {
        this.read_privileges = read_privileges;
    }

    public List<String> getWrite_privileges() {
        return write_privileges;
    }

    public void setWrite_privileges(List<String> write_privileges) {
        this.write_privileges = write_privileges;
    }

    public List<DmRowPermission> getRow_security() {
        return row_security;
    }

    public void setRow_security(List<DmRowPermission> row_security) {
        this.row_security = row_security;
    }

    @Override
    public String toString() {
        return "DmPrivileges{" +
                "read_privileges=" + read_privileges +
                ", write_privileges=" + write_privileges +
                ", row_security=" + row_security +
                '}';
    }
}
