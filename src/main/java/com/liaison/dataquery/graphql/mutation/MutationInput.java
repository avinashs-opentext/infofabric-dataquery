/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.mutation;

import java.util.HashMap;
import java.util.Map;

public class MutationInput extends HashMap<String, Object> {

    private final String type;

    public MutationInput(String type, Map<String, Object> input) {
        super(input);
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
