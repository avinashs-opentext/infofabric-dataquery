/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.query;

import com.opentext.infofabric.dataquery.graphql.RootDataAccess;
import com.opentext.infofabric.dataquery.graphql.RootDataFetcher;
import com.opentext.infofabric.dataquery.dto.AccessPrivileges;
import com.opentext.infofabric.dataquery.graphql.GraphQLService;

import java.util.Map;

public class QueryContext {

    private final RootDataAccess.QueryModel queryModel;
    private Map<String, Object> variables;
    private String tenant;
    private String datamodel;
    private String datamodelBranch;
    private GraphQLService.ViewType viewType;
    private RootDataFetcher.QueryType queryType;
    private String token ;
    private AccessPrivileges accessPrivileges;

    public QueryContext(RootDataAccess.QueryModel queryModel, String tenant, String datamodel, String datamodelBranch, GraphQLService.ViewType viewType, Map<String, Object> variables) {
        this.tenant = tenant;
        this.datamodel = datamodel;
        this.datamodelBranch = datamodelBranch;
        this.viewType = viewType;
        this.variables = variables;
        this.queryModel = queryModel;
    }

    public QueryContext(RootDataAccess.QueryModel queryModel, String tenant, String datamodel, String datamodelBranch, GraphQLService.ViewType viewType, Map<String, Object> variables, String token) {
        this.tenant = tenant;
        this.datamodel = datamodel;
        this.datamodelBranch = datamodelBranch;
        this.viewType = viewType;
        this.variables = variables;
        this.queryModel = queryModel;
        this.token = token;
    }
    
    public QueryContext(RootDataAccess.QueryModel queryModel, String tenant, String datamodel, String datamodelBranch, GraphQLService.ViewType viewType, Map<String, Object> variables, String token,AccessPrivileges accessPrivileges) {
        this.tenant = tenant;
        this.datamodel = datamodel;
        this.datamodelBranch = datamodelBranch;
        this.viewType = viewType;
        this.variables = variables;
        this.queryModel = queryModel;
        this.token = token;
        this.accessPrivileges = accessPrivileges;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getDatamodel() {
        return datamodel;
    }

    public void setDatamodel(String datamodel) {
        this.datamodel = datamodel;
    }

    public GraphQLService.ViewType getViewType() {
        return viewType;
    }

    public void setViewType(GraphQLService.ViewType viewType) {
        this.viewType = viewType;
    }

    public void setQueryType(RootDataFetcher.QueryType queryType) {
        this.queryType = queryType;
    }

    public RootDataFetcher.QueryType getQueryType() {
        return queryType;
    }

    public String getDatamodelBranch() {
        return datamodelBranch;
    }

    public void setDatamodelBranch(String datamodelBranch) {
        this.datamodelBranch = datamodelBranch;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public RootDataAccess.QueryModel getQueryModel() {
        return queryModel;
    }

	public AccessPrivileges getAccessPrivileges() {
		return accessPrivileges;
	}

	public void setAccessPrivileges(AccessPrivileges accessPrivileges) {
		this.accessPrivileges = accessPrivileges;
	}

	@Override
	public String toString() {
		return "QueryContext [queryModel=" + queryModel + ", variables=" + variables + ", tenant=" + tenant
				+ ", datamodel=" + datamodel + ", datamodelBranch=" + datamodelBranch + ", viewType=" + viewType
				+ ", queryType=" + queryType + ", token=" + token + ", accessPrivileges=" + accessPrivileges + "]";
	} 
}
