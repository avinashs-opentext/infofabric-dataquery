/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.cache;

import com.google.inject.Inject;
import com.liaison.dataquery.security.DataqueryRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

public class ResponseCacheFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ResponseCacheFilter.class);

    @Inject
    static ResponseCache cache;

    @Override
    public void init(FilterConfig filterConfig) {
        // Nothing to init.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        DataqueryRequestWrapper requestWrapper = (DataqueryRequestWrapper) request;
        if (!requestWrapper.readFromCache()) {
            chain.doFilter(request, response);
            return;
        }
        Object result = cache.get(requestWrapper.getCacheMapKey(), requestWrapper.getCacheResultKey());
        if (result != null) {
            log.debug("Returning cache hit for cache '{}' key '{}'",
                    requestWrapper.getCacheMapKey(),
                    requestWrapper.getCacheResultKey());
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.getOutputStream().write((byte[]) result);
            httpServletResponse.setStatus(200);
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Nothing to destroy
    }
}
