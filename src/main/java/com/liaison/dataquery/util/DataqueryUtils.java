/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.NamedQueryConfiguration;
import com.liaison.dataquery.dto.AccessPrivileges;
import com.liaison.dataquery.dto.DmFilter;
import com.liaison.dataquery.dto.DmFilterset;
import com.liaison.dataquery.dto.DmPermission;
import com.liaison.dataquery.dto.DmPrivileges;
import com.liaison.dataquery.dto.DmRowPermission;
import com.liaison.dataquery.graphql.query.Filter;
import com.liaison.dataquery.graphql.query.FilterSet;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.liaison.dataquery.DataqueryConstants.SSO_HEADER;

public class DataqueryUtils {

    private static final Logger logger = LoggerFactory.getLogger(DataqueryUtils.class);
    public static final String APPLICATION_ID = "dataquery";
    public static final int CONNECT_TIMEOUT = 5000;

    private DataqueryUtils() {
        logger.info("DataqueryUtils private Constructor");
    }

    private static void parseHttpResponse(DataqueryConfiguration config, HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
            try (InputStream inputStream = response.getEntity().getContent()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                JsonObject jsonResponse = new JsonParser().parse(stringBuilder.toString()).getAsJsonObject();
                setZKLoadBalancerConfig(config, jsonResponse.getAsJsonArray("data").get(0).getAsJsonObject().get("Zookeepers").getAsString());
                return;
            } catch (Exception e) {
                logger.warn("Malformed response, trying next server");
            }
        } else {
            logger.warn("Server responded with {} : {}, trying next server.", statusCode, response.getStatusLine().getReasonPhrase());
        }
    }

    public static void addZooKeeperInfo(DataqueryConfiguration config) throws IOException {
        Map<String, Object> maprAdminClientConfig = config.getDecryptedMaprAdminConfig();
        if (maprAdminClientConfig == null || maprAdminClientConfig.isEmpty()) {
            logger.warn("No MapR Admin config provided.");
            return;
        }
        List<String> servers = Arrays.asList(((String) maprAdminClientConfig.getOrDefault("servers", "")).split(","));
     try(CloseableHttpClient client = HttpClientBuilder.create().build()){
        for (String server : servers) {
            RequestConfig requestConfig = RequestConfig
                    .custom()
                    .setConnectTimeout(CONNECT_TIMEOUT)
                    .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                    .setSocketTimeout(CONNECT_TIMEOUT)
                    .build();
            HttpGet get = new HttpGet(String.format("%s://%s/rest/node/listzookeepers",
                    (Boolean) maprAdminClientConfig.getOrDefault("secure", true) ? "https" : "http", server));
            get.setConfig(requestConfig);
            get.setHeader("Authorization",
                    "Basic " + Base64.encodeBase64String(((String) maprAdminClientConfig.get("user") + ":"
                            + ((String) maprAdminClientConfig.get("pass"))).getBytes()));

           
            try(CloseableHttpResponse response = client.execute(get)) {
                parseHttpResponse(config, response);
                // if one good server responds then we are good so break the loop
                if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300) {
                    break;
                }
            } catch (Exception e) {
                logger.error("Problem connecting to zookeeper server: " + server, e);
            }
        }
     }
    }

    private static void setZKLoadBalancerConfig(DataqueryConfiguration config, String zkServers) {
        Map<String, Object> loadBalancerConfig = config.getLoadBalancerConfig();
        if (loadBalancerConfig == null) {
            loadBalancerConfig = new HashMap<>();
        }
        loadBalancerConfig.put("zookeeperServers", zkServers);
        loadBalancerConfig.put("quorum", getZooKeeperServersWithOutPort(zkServers));
        loadBalancerConfig.put("clientPort", getZooKeeperServerPort(zkServers));
        config.setLoadBalancerConfig(loadBalancerConfig);
    }

    private static String getZooKeeperServersWithOutPort(String str) {
        List<String> l = Arrays.asList(str.split(","));
        StringBuilder servers = new StringBuilder();
        for (String s : l) {
            if (servers.length() > 0) {
                servers.append(",");
            }
            if (!StringUtils.isBlank(s.split(":")[0])) {
                servers.append(s.split(":")[0]);
            }
        }
        logger.info("Zooker servers: {}", servers);
        return servers.toString();
    }

    private static String getZooKeeperServerPort(String str) {
        List<String> l = Arrays.asList(str.split(","));
        if (!l.isEmpty()) {
            return l.get(0).split(":")[1];
        }
        return null;
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
