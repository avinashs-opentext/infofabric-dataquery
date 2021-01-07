/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.util;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.opentext.infofabric.dataquery.dto.DataqueryRequest;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;

import java.io.IOException;
import java.util.Map;

public class DataqueryRequestAdapter extends CustomizedObjectTypeAdapter {

    private static final String QUERY = "query";
    private static final String VARS = "variables";
    private static final String OPNAME = "operationName";

    @Override
    public void write(JsonWriter out, Object value) throws IOException {
        super.write(out, value);
    }

    @Override
    public DataqueryRequest read(JsonReader in) throws IOException {
        DataqueryRequest dataqueryRequest = new DataqueryRequest();
        Object requestBody = super.readAll(in);
        if (requestBody instanceof Map) {
            Map requestMap = (Map) requestBody;
            if (requestMap.get(QUERY) != null) {
                dataqueryRequest.setQuery((String) requestMap.get(QUERY));
            }
            if (requestMap.get(VARS) != null) {
                dataqueryRequest.setVariables((Map<String, Object>) requestMap.get(VARS));
            }
            if (requestMap.get(OPNAME) != null) {
                dataqueryRequest.setOperationName((String) requestMap.get(OPNAME));
            }
        } else {
            throw new DataqueryRuntimeException(
                    String.format("Invalid request body %s expected keys %s, %s, and/or %s.",
                            requestBody,
                            QUERY, VARS, OPNAME));
        }
        return dataqueryRequest;
    }
}
