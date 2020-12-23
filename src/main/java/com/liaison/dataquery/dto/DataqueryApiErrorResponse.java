/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Base HTTP API Error Response POJO
 */
public class DataqueryApiErrorResponse {

    private Object data;
    private List<String> errors = new ArrayList<>();

    public List<String> getErrors() {
        return errors;
    }

    public DataqueryApiErrorResponse setErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    public Object getData() {
        return data;
    }

    public DataqueryApiErrorResponse setData(Object data) {
        this.data = data;
        return this;
    }

}
