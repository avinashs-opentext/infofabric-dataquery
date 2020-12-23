/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.util;

import com.liaison.dataquery.dto.AccessPrivileges;
import com.liaison.dataquery.dto.DmFilter;
import com.liaison.dataquery.dto.DmFilterset;
import com.liaison.dataquery.dto.DmPermission;
import com.liaison.dataquery.dto.DmPrivileges;
import com.liaison.dataquery.dto.DmRowPermission;
import com.liaison.dataquery.graphql.query.Filter;
import com.liaison.dataquery.graphql.query.FilterSet;
import com.liaison.dataquery.guice.GuiceInjector;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GetTablePrivilegesFromCacheTest {

    private String tenant = "unittesttenant";
    private String ssoRole = "mock_role1";
    private String model = "basic_multi_table_01";
    private List<String> ssoRoles = Arrays.asList("mock_role1");

    @Test
    public void getTablePrivilegesFromCacheTest_emptyMap() {
        AccessPrivileges accessPrivileges = DataqueryUtils.getaccessPrivileges(ssoRoles, tenant, model, populatePermissionCache(Collections.emptyList()));
        Assert.assertEquals(Collections.EMPTY_MAP, accessPrivileges.getRowSecurityMap());
    }

    @Test
    public void getTablePrivilegesFromCacheTest_singleEntry(){
        AccessPrivileges accessPrivileges = DataqueryUtils.getaccessPrivileges(ssoRoles, tenant, model, populatePermissionCache(Arrays.asList(
                new DmRowPermission("Author", Collections.emptyList())
        ) ));
        Map<String, FilterSet> assertMap = new HashMap<>();
        FilterSet filterSet = new FilterSet();
        filterSet.initFiltersets();
        assertMap.put("Author", filterSet);
        Assert.assertEquals(assertMap.toString(), accessPrivileges.getRowSecurityMap().toString());
    }

    @Test
    public void getTablePrivilegesFromCacheTest_singleFilterset(){
        AccessPrivileges accessPrivileges = DataqueryUtils.getaccessPrivileges(ssoRoles, tenant, model, populatePermissionCache( Arrays.asList(
                new DmRowPermission("Author", Arrays.asList(
                        new DmFilterset(
                                Arrays.asList(
                                        new DmFilter("name","Hadley Ruggier")
                                )
                        )
                ))
        )));
        Map<String, FilterSet> assertMap = new HashMap<>();
        FilterSet filterSet = new FilterSet();
        filterSet.initFiltersets();
        FilterSet gqlfilterset = new FilterSet();
        gqlfilterset.addFilter(new Filter("name", "", FilterSet.ComparisonOperator.EQ,"Hadley Ruggier", false));
        gqlfilterset.setLogicalOperator(FilterSet.LogicalOperator.OR);
        filterSet.getFiltersets().add(gqlfilterset);
        assertMap.put("Author", filterSet);
        String inputString = assertMap.toString();
        String outputString = accessPrivileges.getRowSecurityMap().toString();
        Assert.assertEquals(inputString, outputString);
    }

    private ConcurrentHashMap<String, DmPermission> populatePermissionCache(List<DmRowPermission> rowSecurityList) {
        ConcurrentHashMap<String, DmPermission> dmPermissionCache = new ConcurrentHashMap<>();
        Map<String, DmPrivileges> models = new HashMap<>();
        models.put( model, new DmPrivileges(Collections.emptyList(),Collections.emptyList(), rowSecurityList));
        dmPermissionCache.put(tenant+"_"+ssoRole , DmPermission.of(tenant,ssoRole,models));
        return dmPermissionCache;
    }
}
