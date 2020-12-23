/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.dto;

import java.util.List;

/**
 * This class represents the FilterSet that would be applied for each row_level_security object
 *
 * @author open text
 */
public class DmFilterset {

    private List<DmFilter> filters;

    public DmFilterset() {
    }

    public DmFilterset(List<DmFilter> filters) {
        this.filters = filters;
    }

    public List<DmFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<DmFilter> filters) {
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "DmFilterset{" +
                "filters=" + filters +
                '}';
    }
}
