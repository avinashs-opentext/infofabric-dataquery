/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.endpoints;

import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.dto.DataqueryApiResponse;
import com.opentext.infofabric.dataquery.dto.DataqueryRequest;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.GraphQLService;
import com.opentext.infofabric.dataquery.guice.GuiceInjector;
import com.opentext.infofabric.dataquery.namedqueries.NamedQuery;
import com.opentext.infofabric.dataquery.services.NamedQueryService;
import graphql.ExecutionResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Path(DataqueryConstants.API_ROOT + "query/named/")
@Api(
        value = DataqueryConstants.API_ROOT + "query/named/",
        tags = "Named Servlet"
)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class NamedQueryServlet extends DataQueryServlet {
    private static final Logger log = LoggerFactory.getLogger(NamedQueryServlet.class);
    private final NamedQueryService namedQueryService;

    public NamedQueryServlet() {
        this.namedQueryService = GuiceInjector.getInjector().getInstance(NamedQueryService.class);
    }

    @POST
    @Path("{tenant}/{model}/{ViewType}")
    @ApiOperation(
            value = "Named",
            notes = "Execute named",
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
    public Response doPost(@PathParam("tenant") final String tenant, @PathParam("model") final String model,
                           @PathParam("ViewType") final String view, String reqBody) {

        try {
            DataqueryRequest dqr = parseRequest(reqBody);
            GraphQLService.ViewType viewType = GraphQLService.ViewType.valueOf(view.toUpperCase());
            if (dqr.getOperationName() != null && (namedQueryService.get(dqr.getOperationName()) == null ||
                    !namedQueryService.get(dqr.getOperationName()).getType().equals(viewType))) {
               return response( new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage("Named query not found"), 404);
            }

            String operation = dqr.getInfo();
            if (operation != null && operation.equals("preview")) {
                Map<String, Object> info = new HashMap<>();
                Object result = namedQueryService.preview(tenant, model, dqr.getOperationName(), dqr.getVariables());
                info.put(dqr.getOperationName(), result);
                return response( info);
            } else if (operation != null && operation.equals("info")) {
                Map<String, Object> info = new HashMap<>();
                NamedQuery result = namedQueryService.get(dqr.getOperationName());
                info.put("variables", result.getVariables());
                info.put("description", result.getDescription());
                info.put("name", result.getName());
                return response( info);
            } else if (operation != null && operation.equals("list")) {
                Map<String, Object> info = new HashMap<>();
                Set<String> result = namedQueryService.getNames(viewType);
                info.put("queries", result);
                return response( info);
            } else {
                ExecutionResult result = namedQueryService.execute(tenant, model, dqr.getOperationName(), dqr.getVariables());
                return response( result);
            }
        } catch (DataqueryRuntimeException dre) {
            log.error(dre.getMessage(), dre);
            return response(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(dre.getMessage()), 400);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return response(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage("Internal Server Error"), 500);
        }
    }

}