/**
 * Copyright 2017 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */

package com.liaison.dataquery.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.dto.DataqueryApiResponse;
import com.liaison.dataquery.exception.DataqueryException;
import com.liaison.sso.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Function;

public class ServletFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ServletFilter.class);

    private static final String QUERY_ROLE = "QUERY";

    private static RestAuthorizer restAuthorizer;

    private static Function<String, String> userFunction ;

    private static boolean enableSSO;

    private Gson gson;

    public static void setEnableSSO(boolean enable) {
        enableSSO = enable;
    }

    public static RestAuthorizer getRestAuthorizer() {
        return restAuthorizer;
    }

    public static void setRestAuthorizer(RestAuthorizer restAuthorizer) {
        ServletFilter.restAuthorizer = restAuthorizer;
    }

    public static void setUserFunction(Function<String, String> userFunction) {
        ServletFilter.userFunction = userFunction;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Inside Servlet Filter Init()");
        gson = new GsonBuilder().create();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse response, FilterChain chain)
            throws IOException {
        logger.debug("Inside Servlet Filter doFilter()");
        try {
            DataqueryRequestWrapper requestWrapper = new DataqueryRequestWrapper((HttpServletRequest) req);
            if (!enableSSO) {
                chain.doFilter(requestWrapper, response);
            } else {

                final String token = requestWrapper.getDataQueryRequest().getToken();
                String model = requestWrapper.getDataQueryRequest().getModel();
                String resourceContext = requestWrapper.getDataQueryRequest().getView().toLowerCase();
                String resource;
                if (resourceContext.equals("rdbms"))
                    resource = model + "_RDBMS";
                else if (resourceContext.equals("hbase"))
                    resource = model + "_HBASE";
                else {
                    logger.error("Invalid view type {} in the request path", resourceContext);
                    throw new DataqueryException(String.format("Invalid view type { %s } in the request path", resourceContext));
                }
                if (!(restAuthorizer.isTokenValid(token))) {
                    logger.warn("Token provided is invalid or expired");
                    ((HttpServletResponse) response).setStatus(401);
                    response.getOutputStream().write(gson.toJson(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage("Unauthorized")).getBytes());
                    return;
                }
                if (authorize(token, requestWrapper.getDataQueryRequest().getTenant(), resource, resourceContext)) {
                    logger.info("Servlet: {} Method: {} - Authorized user: {} Host: {} IP: {}",
                            requestWrapper.getQueryType(),
                            requestWrapper.getMethod(),
                            token != null ? userFunction.apply(token) : "UNKNOWN_USER",
                            requestWrapper.getRemoteHost(),
                            requestWrapper.getRemoteAddr());
                    chain.doFilter(requestWrapper, response);
                } else {
                    logger.warn("Servlet: {} Method: {} - User not Authorized : {} Host: {} IP: {}",
                            requestWrapper.getQueryType(),
                            requestWrapper.getMethod(),
                            token != null ? userFunction.apply(token) : "UNKNOWN_USER",
                            requestWrapper.getRemoteHost(),
                            requestWrapper.getRemoteAddr());
                    ((HttpServletResponse) response).setStatus(403);
                    response.getOutputStream().write(gson.toJson(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage("Forbidden")).getBytes());
                }
            }
        } catch (DataqueryException exc) {
            logger.warn("ServletFilter: Error validating dataquery request : " + exc.getMessage(), exc);
            ((HttpServletResponse) response).setStatus(500);
            response.getOutputStream().write(gson.toJson(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(exc.getMessage())).getBytes());
        } catch (Exception exc) {
            logger.warn("ServletFilter: Error authenticating credentials: " + exc.getMessage(), exc);
            ((HttpServletResponse) response).setStatus(500);
            response.getOutputStream().write(gson.toJson(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(exc.getMessage())).getBytes());
        }
    }

    private boolean authorize(String token, String tenancy, String resource, String resourceContext) {
        /**
         * User Name is not used, so not populating, but this can be retrieve from SSO.
         */
        return restAuthorizer.authorize(new UserInfo("", token, tenancy, resource, resourceContext), QUERY_ROLE);
    }

    @Override
    public void destroy() {
        logger.info("Inside Servlet Filter doFilter()");
    }
}
