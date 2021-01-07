/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.dto;

import java.util.List;

public class NamedQueryRequest {

    private String query;
    private String info;
    private List<NamedQueryVariable> variables;
    private String streamName;
    private String jobId;
    private String queryString;
    private String command;

    private String targetType;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return "NamedQueryRequest{" +
                "query='" + query + '\'' +
                ", info='" + info + '\'' +
                ", variables=" + variables +
                ", command=" + command +
                '}';
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String typeName) {
        this.targetType = typeName;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public List<NamedQueryVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<NamedQueryVariable> variables) {
        this.variables = variables;
    }


    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

}
