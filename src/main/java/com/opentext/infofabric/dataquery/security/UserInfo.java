/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.security;

import java.security.Principal;

public class UserInfo implements Principal {

    private String name;
    private String accessToken;
    private String tenancy;
    private String resource;
    private String resourceContext;

    public UserInfo(String name, String accessToken, String tenancy, String resource, String resourceContext) {
        this.name = name;
        this.accessToken = accessToken;
        this.tenancy = tenancy;
        this.resource = resource;
        this.resourceContext = resourceContext;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTenancy() {
        return tenancy;
    }

    public void setTenancy(String tenancy) {
        this.tenancy = tenancy;
    }

    public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getResourceContext() {
		return resourceContext;
	}

	public void setResourceContext(String resourceContext) {
		this.resourceContext = resourceContext;
	}

	@Override
    public String toString() {
        return "UserInfo{" +
                "name='" + name + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", tenancy='" + tenancy + '\'' +
                ", resource='" + resource + '\'' +
                ", resourceContext='" + resourceContext + '\'' +
                '}';
    }
}
