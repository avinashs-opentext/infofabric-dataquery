/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.util;

import com.opentext.infofabric.dataquery.NamedQueryConfiguration;
import com.opentext.infofabric.dataquery.dto.AccessPrivileges;
import com.opentext.infofabric.dataquery.dto.DmFilter;
import com.opentext.infofabric.dataquery.dto.DmFilterset;
import com.opentext.infofabric.dataquery.dto.DmPermission;
import com.opentext.infofabric.dataquery.dto.DmPrivileges;
import com.opentext.infofabric.dataquery.dto.DmRowPermission;
import com.opentext.infofabric.dataquery.graphql.query.Filter;
import com.opentext.infofabric.dataquery.graphql.query.FilterSet;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.opentext.infofabric.dataquery.DataqueryConstants.SSO_HEADER;

public class DataqueryUtils {

    private static final Logger logger = LoggerFactory.getLogger(DataqueryUtils.class);
    public static final String APPLICATION_ID = "dataquery";
    public static final int CONNECT_TIMEOUT = 5000;

    private DataqueryUtils() {
        logger.info("DataqueryUtils private Constructor");
    }

    public static String getSSOToken(HttpServletRequest req) {
        String rawToken = req.getHeader(SSO_HEADER);
        if (StringUtils.isNotEmpty(rawToken) && rawToken.startsWith("Bearer ")) {
            return rawToken.substring(7);
        }
        return null;
    }

    public static String addQuotes(String field) {
        return String.format("\"%s\"", field);
    }

    public static Date iso8601StringToJavaDate(String dateString) throws ParseException {
        String parsePatterns[] = {
                "yyyy-MM-dd",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ssZZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZZ",

                "yyyyMMdd",
                "yyyyMMdd'T'HHmmss",
                "yyyyMMdd'T'HHmmssZ",
                "yyyyMMdd'T'HHmmssZZ",
                "yyyyMMdd'T'HHmmssSSSZ",
                "yyyyMMdd'T'HHmmssSSSZZ",
                "MM/dd/yyyy"
        };


        Date date = null;
        if (dateString != null) {
            date = DateUtils.parseDateStrictly(dateString, parsePatterns);
        }

        return date;
    }

    public static String getApplicationId() {
        String applicationId = APPLICATION_ID;
        Map<String, Map<String, Object>> appStateConfig = NamedQueryConfiguration.getInstance().getAppState();
        if (appStateConfig.get("application") != null) {
            applicationId = (String) appStateConfig.get("application").getOrDefault("name", APPLICATION_ID);
        }
        return applicationId;
    }

    public static AccessPrivileges getaccessPrivileges(List<String> ssoRoles, String tenant, String model, ConcurrentMap<String, DmPermission> dmPermissionCache) {
        AccessPrivileges accessPrivileges = new AccessPrivileges(tenant, model, new HashSet<>(), new HashSet<>(), new HashMap<>());
        for (String ssoRole : ssoRoles) {
            DmPermission dmPermission = dmPermissionCache.get(tenant + "_" + ssoRole);
            if (dmPermission != null && dmPermission.getTenant().equalsIgnoreCase(tenant)) {
                DmPrivileges privileges = dmPermission.getModels().get(model);
                if (privileges != null) {
                    // removed the individual tableSet structure population based on operator
                    // and populating accessPrivilege values directly in a single call
                    accessPrivileges.getReadPrivilegesTableSet().addAll(privileges.getRead_privileges());
                    accessPrivileges.getWritePrivilegesTableSet().addAll(privileges.getWrite_privileges());
                    Map<String, FilterSet> securityMap =  accessPrivileges.getRowSecurityMap();
                    populateRowSecurity(privileges, securityMap);
                }
            }
        }
        return accessPrivileges;
    }

    private static void populateRowSecurity(DmPrivileges privileges, Map<String, FilterSet> securityMap) {
        FilterSet parentFilterset = null;
        if(!Collections.isEmpty(privileges.getRow_security())) {
            for (DmRowPermission rowPermission : privileges.getRow_security()) {
                if (securityMap.containsKey(rowPermission.getTypeName())) {
                    parentFilterset = securityMap.get(rowPermission.getTypeName());
                } else {
                    parentFilterset = new FilterSet();
                    parentFilterset.initFiltersets();
                    securityMap.put(rowPermission.getTypeName(), parentFilterset);
                }

                for (DmFilterset filterset : rowPermission.getFiltersets()) {
                    FilterSet gqlFilterset = new FilterSet();
                    for (DmFilter filter : filterset.getFilters()) {
                        Filter gqlFilter = new Filter(filter.getColumn(), "", FilterSet.ComparisonOperator.valueOf(filter.getOp()), filter.getValue(), false);
                        gqlFilterset.addFilter(gqlFilter);
                    }
                    gqlFilterset.setLogicalOperator(FilterSet.LogicalOperator.AND);
                    parentFilterset.getFiltersets().add(gqlFilterset);
                }
                parentFilterset.setLogicalOperator(FilterSet.LogicalOperator.OR);
            }
        }
    }
}
