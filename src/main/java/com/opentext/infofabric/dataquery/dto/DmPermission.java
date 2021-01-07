/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.dto;

import java.util.HashMap;
import java.util.Map;

/*
{
	  "_key": "merz_auto_qa_MERZ_DATAQUERY_TEST_ADMIN",
	  "_id": "DM_PERMISSION/merz_auto_qa_MERZ_DATAQUERY_TEST_ADMIN",
	  "dm_role": "DM_MERZ_DATAQUERY_TEST_ADMIN",
	  "tenant": "merz_auto_qa",
	  "models": {
	    "merz_cdw_qa_1": {
	      "read_privileges": [
	        "DW_XRF_BIZ_UNT_BRND",
	        "DW_DIM_BIZ_UNT",
	        "DW_DIM_PROD_ADH"
	      ],
	      "write_privileges": [
	        "DW_XRF_BIZ_UNT_BRND"
	      ]
	    }
	  }
	} 
 */

/**
 * This class represents configuration store documents in Java cache store.
 *
 * @author open text
 */
public class DmPermission {

    private String _key;

    private String dm_role;

    private String tenant;

    private Map<String, DmPrivileges> models = new HashMap<>();

    public DmPermission() {
        super();
    }

    public String get_key() {
        return _key;
    }

    public void set_key(String _key) {
        this._key = _key;
    }

    public String getDm_role() {
        return dm_role;
    }

    public void setDm_role(String dm_role) {
        this.dm_role = dm_role;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public Map<String, DmPrivileges> getModels() {
        return models;
    }

    public void setModels(Map<String, DmPrivileges> models) {
        this.models = models;
    }

    public static DmPermission of(String tenant,
                                  String ssoRole,
                                  Map<String, DmPrivileges> models) {
        DmPermission permission = new DmPermission();
        permission.setTenant(tenant);
        permission.setDm_role(ssoRole);
        permission.setModels(models);
        return permission;
    }

    @Override
    public String toString() {
        return "DmPermission [_key=" + _key + ", dm_role=" + dm_role + ", tenant=" + tenant + ", models=" + models
                + "]";
    }
}
