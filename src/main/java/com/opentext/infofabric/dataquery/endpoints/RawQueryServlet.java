/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.opentext.infofabric.dataquery.DataqueryConfiguration;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.dto.DataqueryApiResponse;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.HBaseQuery;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.model.RawQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;


@Path(DataqueryConstants.API_ROOT + "query/raw/")
@Api(
        value = DataqueryConstants.API_ROOT + "query/raw/",
        tags = "Raw query Servlet"
)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class RawQueryServlet extends DataQueryServlet {

    private static final Logger logger = LoggerFactory.getLogger(RawQueryServlet.class);


    @GET
    @Path("{tenant}/{model}/{ViewType}")
    @ApiOperation(
            value = "GetRawQuery",
            notes = "Get Raw Query.",
            response = DataqueryApiResponse.class
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = "Get Raw Query fails", response = DataqueryApiResponse.class)
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
                      @PathParam("ViewType") final String view) {
        return response(new DataqueryApiResponse().setStatus(DataqueryConstants.SUCCESS).setMessage("This is Ping!!"));
    }

    @POST
    @Path("{tenant}/{model}/{ViewType}")
    @ApiOperation(
            value = "PostRawQuery",
            notes = "Execute POST Raw Query",
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
                           @PathParam("ViewType") final String view, String req) {
        Gson gson = new GsonBuilder().create();
        HBaseQuery hQuery = null;

        try {
            hQuery = new HBaseQuery(tenant, model);
            RawQuery rq = gson.fromJson(req, RawQuery.class);
            String respObj ;
            if (!rq.getRowKeys().isEmpty()) {
                respObj = gson.toJson(hQuery.getByRowKey(rq));
            } else {
                respObj = gson.toJson(hQuery.scan(rq));
            }
            return  Response.ok(respObj).status(HttpServletResponse.SC_OK).build();
        } catch (Exception e) {
            logger.error("Exception in query parsing..", e);
            return response(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage("Exception in query parsing.."));
        } finally {
            if (hQuery != null) {
                hQuery.cleanUp();
            }
        }
    }
}
