/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.endpoints;

import com.google.common.cache.CacheStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.opentext.infofabric.dataquery.DataqueryConfiguration;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.cache.ResponseCache;
import com.opentext.infofabric.dataquery.dto.DataqueryRequest;
import com.opentext.infofabric.dataquery.guice.GuiceInjector;
import com.opentext.infofabric.dataquery.security.DataqueryRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

public class DataQueryServlet {
    private static final Logger log = LoggerFactory.getLogger(DataQueryServlet.class);
    protected final DataqueryConfiguration config;
    protected static Gson json = new GsonBuilder().serializeNulls().create();

    @Inject
    private static ResponseCache cache;

    /**
     * Default Constructor
     */
    public DataQueryServlet() {
        this.config = GuiceInjector.getInjector().getInstance(DataqueryConfiguration.class);
    }

    protected Response response(Object obj) {
        return response(obj, HttpServletResponse.SC_OK);
    }

    protected Response response(Object obj, int status) {
        return Response.ok(obj).status(status).build();
    }

    public static Map<String, CacheStats> getCacheStats() {
        if (cache == null) {
            return null;
        }
        return cache.getStats();
    }

    public DataqueryRequest parseRequest(String input) {
        return json.fromJson(input, DataqueryRequest.class);
    }
}
