/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.endpoints;

import com.opentext.infofabric.appstate.client.AppStateClient;
import com.opentext.infofabric.common.dropwizard.security.UserInfo;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.dto.AccessPrivileges;
import com.opentext.infofabric.dataquery.dto.DataqueryApiErrorResponse;
import com.opentext.infofabric.dataquery.dto.DataqueryApiResponse;
import com.opentext.infofabric.dataquery.dto.DataqueryRequest;
import com.opentext.infofabric.dataquery.dto.DmModel;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.GraphQLService;
import com.opentext.infofabric.dataquery.graphql.RootDataAccess;
import com.opentext.infofabric.dataquery.guice.GuiceInjector;
import com.opentext.infofabric.dataquery.services.ModelService;
import com.opentext.infofabric.dataquery.util.AppStateService;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import io.dropwizard.auth.Auth;
import io.prometheus.client.Histogram;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static com.opentext.infofabric.dataquery.DataqueryConstants.PROMETHEUS_METRICS_ROOT;

@Path(DataqueryConstants.API_ROOT + "query/graphql/")
@Api(
        value = DataqueryConstants.API_ROOT + "query/graphql/",
        tags = "GraphQL Servlet"
)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
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
    private AppStateClient appStateClient;

    private final static String DEFAULT_BRANCH = "master";

    public GraphQLServlet() {
        this.modelService = GuiceInjector.getInjector().getInstance(ModelService.class);
        this.graphQLService = GuiceInjector.getInjector().getInstance(GraphQLService.class);
        this.appStateClient = GuiceInjector.getInjector().getInstance(AppStateClient.class);
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
    @Consumes(MediaType.TEXT_PLAIN)
    public void doPost(@PathParam("tenant") final String tenant, @PathParam("model") final String model,
                       @PathParam("ViewType") final String view, @Auth UserInfo userInfo, @QueryParam("refresh") boolean refresh, String query, final @Suspended AsyncResponse response) {
        Histogram.Timer dbQueryTimer = null;
        Histogram.Timer graphQlRequestTimer = graphQlTimer.startTimer();
        try {
            DataqueryRequest dqr = parseRequest(query);
            dbQueryTimer = getQueryRequestTimer(dbQueryTimer, view);
            
            //add the Access privilege for the object security types check
            AccessPrivileges accessPrivileges = new AccessPrivileges();
            if (userInfo.getAccessToken() != null && tenant != null) {
//                List<String> ssoRoles = dataGateSecurity.getRoles(dqr.getToken(), dqr.getTenant());
                List<String> ssoRoles = populateSsoroles();
                if (log.isDebugEnabled()) {
                    log.debug("SSO ROLES of login User:: " + ssoRoles);
                }
                accessPrivileges =  AppStateService.getTablePrivilegesFromCache(ssoRoles, tenant, model);
            }

            if (!graphQLService.hasService(tenant, model, DEFAULT_BRANCH)) {
                // passing in null for branch and version. branch will default to master and version will default to latest
                String sdl = modelService.getSDLByModel(userInfo.getAccessToken(), tenant, model, null, null);
                graphQLService.createService(tenant, model, DEFAULT_BRANCH, sdl);
            }
            ExecutionResult result = graphQLService.execute(userInfo.getAccessToken(),tenant, model, DEFAULT_BRANCH,
                    dqr.getQuery(),
                    dqr.getVariables(),
                    GraphQLService.ViewType.valueOf(view.toUpperCase()),
                    RootDataAccess.QueryModel.SINGLE_QUERY,
                    accessPrivileges);
            if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                setErrorResponseForGraphQLErrors(result);
            } else {
                response.resume(response(result, 200));
            }
        } catch (DataqueryRuntimeException dre) {
            log.error(dre.getMessage(), dre);
            response.resume(response(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(dre.getMessage()), 400));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.resume(response( new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(e.getMessage()), 500));
        } finally {
            if (dbQueryTimer != null) {
                dbQueryTimer.observeDuration();
            }
            graphQlRequestTimer.observeDuration();
        }
    }

    //TODO
    private List<String> populateSsoroles() {
        return Arrays.asList(
                "thermofisher_dev_dataquery_admin",
                "refui_dataquery_admin",
                "yourco_dataquery_admin",
                "MERZ_DATAQUERY_ADMIN_QA",
                "yourco_datacast_admin",
                "thermofisher_test_dataquery_admin",
                "THERMOFISHER_DATAQUERY_ADMIN",
                "coke_dev_dataquery_admin",
                "datamodel_Admin",
                "thermo_dataquery_admin",
                "retail_dataquery_admin");
    }

    private void setErrorResponseForGraphQLErrors(ExecutionResult result) {
        DataqueryApiErrorResponse dataqueryApiErrorResponse = new DataqueryApiErrorResponse();
        dataqueryApiErrorResponse.setData(result.getData());
        for (GraphQLError graphQLError : result.getErrors()) {
            log.error(graphQLError.getMessage());
            dataqueryApiErrorResponse.getErrors().add(graphQLError.getMessage());
        }
        response(dataqueryApiErrorResponse, 400);
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
    public Response doGet(@PathParam("tenant") final String tenant, @PathParam("model") final String model,
                          @PathParam("ViewType") final String view, @ApiParam(hidden = true) @Auth UserInfo userInfo, @QueryParam("refresh") boolean isRefresh) {
        Histogram.Timer timer = graphQlGetSdlTimer.startTimer();

        try {

            String sdl;

			if (isRefresh) {
				DmModel data = new DmModel();
				data.setModel(model);
				data.setTenant(tenant);
				String key = tenant + "_" + model;
				// asynchronous call back to update pods
				appStateClient.update(DataqueryConstants.DM_MODEL, key, data);
				// to show the updated schema to the user request
				sdl = modelService.getSDLByModel(userInfo.getAccessToken(), tenant, model, null, null);
				graphQLService.createService(tenant, model, DEFAULT_BRANCH, sdl);
			} else if (!graphQLService.hasService(tenant, model, DEFAULT_BRANCH)) {
				// passing in null for branch and version. branch will default to master and
				// version will default to latest
				// schema does not present in the cache 
				sdl = modelService.getSDLByModel(userInfo.getAccessToken(), tenant, model, null, null);
				graphQLService.createService(tenant, model, DEFAULT_BRANCH, sdl);
			} else {
				// schema presents in the cache
				sdl = graphQLService.getSDL(tenant,model, DEFAULT_BRANCH);
			}

            return response(sdl);
        } catch (DataqueryRuntimeException dre) {
            log.error(dre.getMessage(), dre);
            return response(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(dre.getMessage()), 400);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return response(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(e.getMessage()), 500);
        } finally {
            timer.observeDuration();
        }
    }

    private Histogram.Timer getQueryRequestTimer(Histogram.Timer dbQueryTimer,String view) {
        if ("RDBMS".equalsIgnoreCase(view)) {
            dbQueryTimer = graphQlRdbmsQueryRequestTimer.startTimer();
        } else if ("HBASE".equalsIgnoreCase(view)) {
            dbQueryTimer = graphQlHbaseQueryRequestTimer.startTimer();
        }
        return dbQueryTimer;
    }

}