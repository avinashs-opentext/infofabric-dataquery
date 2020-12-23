/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.cache.ResponseCache;
import com.liaison.dataquery.dto.DataqueryRequest;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.graphql.RootDataAccess;
import com.liaison.dataquery.util.CustomizedObjectTypeAdapter;
import com.liaison.dataquery.util.DataqueryRequestAdapter;
import com.liaison.dataquery.util.DataqueryUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.liaison.dataquery.DataqueryConstants.SEPARATOR;

public class DataqueryRequestWrapper extends HttpServletRequestWrapper {

    private static final Logger log = LoggerFactory.getLogger(DataqueryRequestWrapper.class);
    private static final String CACHE_CONTROL = "cache-control";
    private static final String NO_CACHE = "no-cache";
    private static final String NO_STORE = "no-store";
    private static final String CACHE_PUBLIC = "public";
    private static final String CACHE_PRIVATE = "private";
    private static final String QUERY_MODEL = "x-query-model";
    private final DataqueryRequest dataQueryRequest;
    private static CustomizedObjectTypeAdapter adapter = new DataqueryRequestAdapter();
    private static Gson json = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(DataqueryRequest.class, adapter)
            .create();
    private Object cacheResultKey;
    private boolean cacheResult;
    private String cacheMapKey;
    private boolean readCache;
    private String queryType;
    private RootDataAccess.QueryModel queryModel = null;

    /**
     * Creates a ServletRequest adaptor wrapping the given request object.
     *
     * @param request
     * @throws IllegalArgumentException if the request is null
     */
    public DataqueryRequestWrapper(HttpServletRequest request) {
        super(request);
        this.dataQueryRequest = parse();
        setDataqueryMeta();
        setCacheMeta();
    }

    public DataqueryRequest getDataQueryRequest() {
        return dataQueryRequest;
    }

    public boolean readFromCache() {
        return readCache;
    }

    public boolean writeToCache() {
        return cacheResult;
    }

    public String getCacheMapKey() {
        return cacheMapKey;
    }

    public Object getCacheResultKey() {
        return cacheResultKey;
    }

    public String getQueryType() {
        return queryType;
    }

    private DataqueryRequest parse() {
        DataqueryRequest dataqueryRequest = null;
        try (ServletInputStream is = this.getInputStream()) {
            if (is != null) {
                dataqueryRequest = json.fromJson(IOUtils.toString(is, "UTF-8"), DataqueryRequest.class);
            }
            if (dataqueryRequest == null) {
                return new DataqueryRequest();
            }
            return dataqueryRequest;
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            throw new DataqueryRuntimeException("Failed to get input stream from http request.", ioe);
        } catch (JsonSyntaxException jse) {
            log.error(jse.getMessage(), jse);
            throw new DataqueryRuntimeException("JSON data is invalid in the request.", jse);
        }
    }

    private void setDataqueryMeta() {
        dataQueryRequest.setToken(DataqueryUtils.getSSOToken(this));
        if (StringUtils.isNotEmpty(this.getServletPath())) {
            String[] servletPath = this.getServletPath().split(SEPARATOR);
            this.queryType = StringUtils.upperCase(servletPath[servletPath.length - 1]);
        }
        if (StringUtils.isNotEmpty(this.getPathInfo())) {
            String[] tokenizer = this.getPathInfo().split(SEPARATOR);
            if (tokenizer.length < DataqueryConstants.PATH_PARAMS.INFO.value() ||
                    tokenizer.length > (DataqueryConstants.PATH_PARAMS.INFO.value() + 1)) {
                throw new DataqueryRuntimeException("The URL path is required in this format: /{{tenant}}/{{model}}/{{view}}.");
            }
            dataQueryRequest.setTenant(tokenizer[DataqueryConstants.PATH_PARAMS.TENANT.value()]);
            dataQueryRequest.setModel(tokenizer[DataqueryConstants.PATH_PARAMS.MODEL.value()]);
            dataQueryRequest.setView(tokenizer[DataqueryConstants.PATH_PARAMS.VIEW.value()]);
            if (tokenizer.length > DataqueryConstants.PATH_PARAMS.INFO.value()) {
                dataQueryRequest.setInfo(tokenizer[DataqueryConstants.PATH_PARAMS.INFO.value()]);
            }
            String queryModel = getHeader(QUERY_MODEL);
            if(StringUtils.isNotEmpty(queryModel)){
                this.queryModel = RootDataAccess.QueryModel.valueOf(queryModel);
            }
        } else {
            throw new DataqueryRuntimeException("The URL path is required in this format: /{{tenant}}/{{model}}/{{view}}.");
        }
    }

    private void setCacheMeta() {
        String cacheHeader = getHeader(CACHE_CONTROL);
        if (cacheHeader == null || dataQueryRequest.getQuery() == null) {
            return;
        }
        if (!cacheHeader.contains(CACHE_PUBLIC) && !cacheHeader.contains(CACHE_PRIVATE)) {
            return;
        }
        if (!cacheHeader.contains(NO_CACHE)) {
            readCache = true;
        }
        if (!cacheHeader.contains(NO_STORE)) {
            cacheResult = true;
        }
        if (!readCache && !cacheResult) {
            // User explicitly opted out from using cache.
            return;
        }

        if (cacheHeader.contains(CACHE_PUBLIC)) {
            this.cacheMapKey = ResponseCache.getCacheIdentifier(
                    dataQueryRequest.getTenant(),
                    dataQueryRequest.getModel(),
                    dataQueryRequest.getView());
        } else if (cacheHeader.contains(CACHE_PRIVATE)) {
            if (StringUtils.isEmpty(dataQueryRequest.getToken())) {
                cacheResult = false;
                readCache = false;
                log.warn("Private cache disabled when SSO token not available.");
                return;
            }
            this.cacheMapKey = ResponseCache.getCacheIdentifier(
                    dataQueryRequest.getTenant(),
                    dataQueryRequest.getModel(),
                    dataQueryRequest.getView(),
                    dataQueryRequest.getToken());
        }

        this.cacheResultKey = dataQueryRequest.getQuery();
        if (dataQueryRequest.getVariables() != null) {
            dataQueryRequest.getVariables().forEach((k, v) -> this.cacheResultKey += String.join("=", k, String.valueOf(v)));
        }
        log.debug("Built cache meta data with map key: {} and result key: {}", cacheMapKey, cacheResultKey);
    }

    public RootDataAccess.QueryModel getQueryModel() {
        return queryModel;
    }

}
