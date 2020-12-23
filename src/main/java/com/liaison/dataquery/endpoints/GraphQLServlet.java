/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.endpoints;

import com.google.inject.Inject;
import com.liaison.datagate.fcl.security.DataGateSecurity;
import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.dto.AccessPrivileges;
import com.liaison.dataquery.dto.DataqueryApiErrorResponse;
import com.liaison.dataquery.dto.DataqueryApiResponse;
import com.liaison.dataquery.dto.DataqueryRequest;
import com.liaison.dataquery.dto.DmModel;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.graphql.GraphQLService;
import com.liaison.dataquery.security.DataqueryRequestWrapper;
import com.liaison.dataquery.services.ModelService;
import com.liaison.dataquery.util.AppStateService;

import com.opentext.infofabric.appstate.client.AppStateClient;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import io.prometheus.client.Histogram;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

import static com.liaison.dataquery.DataqueryConstants.CONTENT_DISPOSITION;
import static com.liaison.dataquery.DataqueryConstants.CONTENT_TYPE_JSON;
import static com.liaison.dataquery.DataqueryConstants.CONTENT_TYPE_TEXT;
import static com.liaison.dataquery.DataqueryConstants.PROMETHEUS_METRICS_ROOT;

@Path(DataqueryConstants.API_ROOT + "query/graphql/")
@Api(
        value = DataqueryConstants.API_ROOT + "query/graphql/",
        tags = "GraphQL Servlet"
)
@Produces(MediaType.APPLICATION_JSON)
public class GraphQLServlet extends DataQueryServlet {
    private static final Logger log = LoggerFactory.getLogger(GraphQLServlet.class);

    // metrics for the latency of Query requests
    static final Histogram graphQlTimer = Histogram.build()
            .name(PROMETHEUS_METRICS_ROOT + "graphql_query_requests_latency_seconds")
            .help("GraphQLServlet Query Request latency in seconds.")
            .register();
    // metrics for GetSDL requests
    static final Histogram graphQlGetSdlTimer = Histogram.build()
            .name(PROMETHEUS_METRICS_ROOT + "graphql_get_sdl_requests_latency_seconds")
            .help("GraphQLServlet Get SDL Request latency in seconds.")
            .register();
    // metrics for RDBMS request
    static final Histogram graphQlRdbmsQueryRequestTimer = Histogram.build()
            .name(PROMETHEUS_METRICS_ROOT + "graphql_rdbms_requests_latency_seconds")
            .help("GraphQLServlet RDBMS Query Request latency in seconds.")
            .register();
    // metrics for HBASE request
    static final Histogram graphQlHbaseQueryRequestTimer = Histogram.build()
            .name(PROMETHEUS_METRICS_ROOT + "graphql_hbase_requests_latency_seconds")
            .help("GraphQLServlet HBASE Query Request latency in seconds.")
            .register();


    private final ModelService modelService;
    private final GraphQLService graphQLService;
    private DataGateSecurity dataGateSecurity;
    private AppStateClient appStateClient;

    private final static String DEFAULT_BRANCH = "master";

    @Inject
    public GraphQLServlet(DataqueryConfiguration config, ModelService modelService, GraphQLService graphQLService,DataGateSecurity dataGateSecurity,AppStateClient appStateClient) {
        super(config);
        this.modelService = modelService;
        this.graphQLService = graphQLService;
        this.dataGateSecurity=dataGateSecurity;
        this.appStateClient=appStateClient;
    }

    @POST
    @Path("{tenant}/{model}/{ViewType}")
    @ApiOperation(
            value = "Graphql",
            notes = "Execute graphql",
            response = String.class
    )
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization", required = true, dataType = "string", paramType =
                    "header"),
            @ApiImplicitParam(name = "tenant", value = "tenant", required = true, dataType = "string", paramType =
                    "path"),
            @ApiImplicitParam(name = "model", value = "model", required = true, dataType = "string", paramType =
                    "path"),
            @ApiImplicitParam(name = "ViewType", value = "ViewType", required = true, dataType = "string", paramType
                    = "path")
    })
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse resp) {
        Histogram.Timer dbQueryTimer = null;
        Histogram.Timer graphQlRequestTimer = graphQlTimer.startTimer();
        resp.setContentType(CONTENT_TYPE_JSON);
        DataqueryRequestWrapper requestWrapper = (DataqueryRequestWrapper) request;
        try {
            DataqueryRequest dqr = requestWrapper.getDataQueryRequest();
            dbQueryTimer = getQueryRequestTimer(dbQueryTimer, dqr);
            
            //add the Access privilege for the object security types check
            AccessPrivileges accessPrivileges = new AccessPrivileges();
            if (dqr.getToken() != null && dqr.getTenant() != null) {
                List<String> ssoRoles = dataGateSecurity.getRoles(dqr.getToken(), dqr.getTenant());
                if (log.isDebugEnabled()) {
                    log.debug("SSO ROLES of login User:: " + ssoRoles);
                }
                accessPrivileges =  AppStateService.getTablePrivilegesFromCache(ssoRoles, dqr.getTenant(), dqr.getModel());
            }

            if (!graphQLService.hasService(dqr.getTenant(), dqr.getModel(), DEFAULT_BRANCH)) {
                // passing in null for branch and version. branch will default to master and version will default to latest
                String sdl = modelService.getSDLByModel(dqr.getToken(), dqr.getTenant(), dqr.getModel(), null, null);
                graphQLService.createService(dqr.getTenant(), dqr.getModel(), DEFAULT_BRANCH, sdl);
            }
            ExecutionResult result = graphQLService.execute(dqr.getToken(),dqr.getTenant(),
                    dqr.getModel(), DEFAULT_BRANCH,
                    dqr.getQuery(),
                    dqr.getVariables(),
                    GraphQLService.ViewType.valueOf(dqr.getView().toUpperCase()),
                    requestWrapper.getQueryModel(),
                    accessPrivileges);
            if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                setErrorResponseForGraphQLErrors(resp, result);
            } else {
                response(requestWrapper, resp, result, 200);
            }
        } catch (DataqueryRuntimeException dre) {
            log.error(dre.getMessage(), dre);
            response(resp, new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(dre.getMessage()), 400);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response(resp, new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(e.getMessage()), 500);
        } finally {
            if (dbQueryTimer != null) {
                dbQueryTimer.observeDuration();
            }
            graphQlRequestTimer.observeDuration();
        }
    }

    private void setErrorResponseForGraphQLErrors(HttpServletResponse resp, ExecutionResult result) {
        DataqueryApiErrorResponse dataqueryApiErrorResponse = new DataqueryApiErrorResponse();
        dataqueryApiErrorResponse.setData(result.getData());
        for (GraphQLError graphQLError : result.getErrors()) {
            log.error(graphQLError.getMessage());
            dataqueryApiErrorResponse.getErrors().add(graphQLError.getMessage());
        }
        response(resp, dataqueryApiErrorResponse, 400);
    }

    @GET
    @Path("{tenant}/{model}/{ViewType}")
    @ApiOperation(
            value = "Graphql",
            notes = "Establishes session.",
            response = DataqueryApiResponse.class
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = "Graphql fails", response = DataqueryApiResponse.class)
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization", required = true, dataType = "string", paramType =
                    "header"),
            @ApiImplicitParam(name = "tenant", value = "tenant", required = true, dataType = "string", paramType =
                    "path"),
            @ApiImplicitParam(name = "model", value = "model", required = true, dataType = "string", paramType =
                    "path"),
            @ApiImplicitParam(name = "ViewType", value = "ViewType", required = true, dataType = "string", paramType
                    = "path")
    })
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Histogram.Timer timer = graphQlGetSdlTimer.startTimer();
        resp.setContentType(CONTENT_TYPE_TEXT);
        resp.setHeader("Content-Disposition", CONTENT_DISPOSITION);

        try {
            DataqueryRequest dqr = ((DataqueryRequestWrapper) req).getDataQueryRequest();

            String sdl;
            boolean isRefresh = Boolean.parseBoolean(req.getParameter("refresh"));
			String tenant = dqr.getTenant();
			String model = dqr.getModel();

			if (isRefresh) {
				DmModel data = new DmModel();
				data.setModel(model);
				data.setTenant(tenant);
				String key = tenant + "_" + model;
				// asynchronous call back to update pods
				appStateClient.update(DataqueryConstants.DM_MODEL, key, data);
				// to show the updated schema to the user request
				sdl = modelService.getSDLByModel(dqr.getToken(), dqr.getTenant(), dqr.getModel(), null, null);
				graphQLService.createService(dqr.getTenant(), dqr.getModel(), DEFAULT_BRANCH, sdl);
			} else if (!graphQLService.hasService(tenant, model, DEFAULT_BRANCH)) {
				// passing in null for branch and version. branch will default to master and
				// version will default to latest
				// schema does not present in the cache 
				sdl = modelService.getSDLByModel(dqr.getToken(), dqr.getTenant(), dqr.getModel(), null, null);
				graphQLService.createService(dqr.getTenant(), dqr.getModel(), DEFAULT_BRANCH, sdl);
			} else {
				// schema presents in the cache
				sdl = graphQLService.getSDL(dqr.getTenant(), dqr.getModel(), DEFAULT_BRANCH);
			}

            response(resp, sdl);
        } catch (DataqueryRuntimeException dre) {
            log.error(dre.getMessage(), dre);
            response(resp, new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(dre.getMessage()), 400);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response(resp, new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(e.getMessage()), 500);
        } finally {
            timer.observeDuration();
        }
    }

    private Histogram.Timer getQueryRequestTimer(Histogram.Timer dbQueryTimer, DataqueryRequest dqr) {
        if ("RDBMS".equalsIgnoreCase(dqr.getView())) {
            dbQueryTimer = graphQlRdbmsQueryRequestTimer.startTimer();
        } else if ("HBASE".equalsIgnoreCase(dqr.getView())) {
            dbQueryTimer = graphQlHbaseQueryRequestTimer.startTimer();
        }
        return dbQueryTimer;
    }
}