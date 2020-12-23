/**
 * Copyright 2019 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.results;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ResultObject extends HashMap<String, Object> implements Comparable {
    private String hash = null;

    // Optional constructor with hash if ordering is important
    public ResultObject(String hash) {
        super();
        this.hash = hash;
    }

    public ResultObject(Map<String,Object> m) {
        super(m);
    }

    public ResultObject(int columns) {
        super(columns);
    }

    public ResultObject() {
        super();
    }

    @Override
    public int hashCode() {
        if (StringUtils.isNotEmpty(hash)) {
            return hash.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        }
        return Integer.compare(this.hashCode(), o.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (StringUtils.isNotEmpty(this.hash)) {
            return this.hash.hashCode() == o.hashCode();
        }
        return super.equals(o);
    }
}
