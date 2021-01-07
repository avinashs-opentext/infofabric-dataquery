/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.dto;

/**
 * Base HTTP API Response POJO
 */
public class NamedQueryApiResponse {

    private String status;
    private String message;
    private Object result;

    public String getStatus() {
        return status;
    }

    public NamedQueryApiResponse setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public NamedQueryApiResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public NamedQueryApiResponse setResult(Object result) {
        this.result = result;
        return this;
    }

    public Object getResult() {
        return result;
    }
}
