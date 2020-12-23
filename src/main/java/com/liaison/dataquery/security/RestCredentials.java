/**
 * Copyright 2017 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.security;

public class RestCredentials {

    private final String accessToken;
    private final String tenancy;
    private String resource;
    private String resourceContext;

    public RestCredentials(String token, String tenancy, String resource, String resourceContext) {
        this.accessToken = token;
        this.tenancy = tenancy;
        this.resource = resource;
        this.resourceContext = resourceContext;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTenancy() {
        return tenancy;
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
    
}
