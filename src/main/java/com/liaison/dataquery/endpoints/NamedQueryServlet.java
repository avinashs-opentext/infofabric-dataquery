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
import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.dto.DataqueryApiResponse;
import com.liaison.dataquery.dto.DataqueryRequest;
import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.graphql.GraphQLService;
import com.liaison.dataquery.namedqueries.NamedQuery;
import com.liaison.dataquery.security.DataqueryRequestWrapper;
import com.liaison.dataquery.services.NamedQueryService;
import graphql.ExecutionResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.liaison.dataquery.DataqueryConstants.CONTENT_TYPE_JSON;

@Path(DataqueryConstants.API_ROOT + "query/named/")
@Api(
        value = DataqueryConstants.API_ROOT + "query/named/",
        tags = "Named Servlet"
)
@Produces(MediaType.APPLICATION_JSON)
public class NamedQueryServlet extends DataQueryServlet {
    private static final Logger log = LoggerFactory.getLogger(NamedQueryServlet.class);
    private final NamedQueryService namedQueryService;

    @Inject
    public NamedQueryServlet(DataqueryConfiguration config, NamedQueryService namedQueryService) {
        super(config);
        this.namedQueryService = namedQueryService;
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
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(CONTENT_TYPE_JSON);

        try {
            DataqueryRequest dqr = ((DataqueryRequestWrapper) req).getDataQueryRequest();

            GraphQLService.ViewType viewType = GraphQLService.ViewType.valueOf(dqr.getView().toUpperCase());
            if (dqr.getOperationName() != null && (namedQueryService.get(dqr.getOperationName()) == null ||
                    !namedQueryService.get(dqr.getOperationName()).getType().equals(viewType))) {
                response(resp, new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage("Named query not found"), 404);
                return;
            }

            String operation = dqr.getInfo();
            if (operation != null && operation.equals("preview")) {
                Map<String, Object> info = new HashMap<>();
                Object result = namedQueryService.preview(dqr.getTenant(), dqr.getModel(), dqr.getOperationName(), dqr.getVariables());
                info.put(dqr.getOperationName(), result);
                response(resp, info);
            } else if (operation != null && operation.equals("info")) {
                Map<String, Object> info = new HashMap<>();
                NamedQuery result = namedQueryService.get(dqr.getOperationName());
                info.put("variables", result.getVariables());
                info.put("description", result.getDescription());
                info.put("name", result.getName());
                response(resp, info);
            } else if (operation != null && operation.equals("list")) {
                Map<String, Object> info = new HashMap<>();
                Set<String> result = namedQueryService.getNames(viewType);
                info.put("queries", result);
                response(resp, info);
            } else {
                ExecutionResult result = namedQueryService.execute(dqr.getTenant(), dqr.getModel(), dqr.getOperationName(), dqr.getVariables());
                response(resp, result);
            }
        } catch (DataqueryRuntimeException dre) {
            log.error(dre.getMessage(), dre);
            response(resp, new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(dre.getMessage()), 400);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response(resp, new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage("Internal Server Error"), 500);
        }
    }

}