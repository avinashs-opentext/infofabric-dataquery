/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.dto.DataqueryApiResponse;
import com.liaison.dataquery.graphql.dataloaders.hbase.HBaseQuery;
import com.liaison.dataquery.graphql.dataloaders.hbase.model.RawQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;


@Path(DataqueryConstants.API_ROOT + "query/raw/")
@Api(
        value = DataqueryConstants.API_ROOT + "query/raw/",
        tags = "Raw query Servlet"
)
@Produces(MediaType.APPLICATION_JSON)
public class RawQueryServlet extends DataQueryServlet {

    private static final Logger logger = LoggerFactory.getLogger(RawQueryServlet.class);

    /**
     * Default Constructor
     */
    @Inject
    public RawQueryServlet(DataqueryConfiguration config) {
        super(config);
    }

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
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        Gson gson = new GsonBuilder().create();

        resp.setStatus(HttpServletResponse.SC_OK);
        try {
            resp.getOutputStream().write(gson.toJson(new DataqueryApiResponse().setStatus(DataqueryConstants.SUCCESS).setMessage("This is Ping!!")).getBytes());
        } catch (IOException ioe) {
            logger.error("Eexception while writing response", ioe);
            resp.getOutputStream().write(gson.toJson(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage("Exception Occured..")).getBytes());
        }
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
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        Gson gson = new GsonBuilder().create();
        HBaseQuery hQuery = null;

        try {
            ServletInputStream is = req.getInputStream();
            byte[] data = new byte[is.available()];
            is.read(data);
            is.close();

            String pathInfo = req.getPathInfo();
            String[] tokens = pathInfo.split("/");

            hQuery = new HBaseQuery(tokens[1], tokens[2]);

            RawQuery rq = gson.fromJson(new String(data), RawQuery.class);
            if (!rq.getRowKeys().isEmpty()) {
                resp.getOutputStream().write(
                        gson.toJson(
                                hQuery.getByRowKey(rq)
                        ).getBytes());
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.getOutputStream().write(
                        gson.toJson(
                                hQuery.scan(rq)
                        ).getBytes());
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (Exception e) {
            logger.error("Exception in query parsing..", e);
            try {
                resp.getOutputStream().write(gson.toJson(new DataqueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage("Exception in query parsing..")).getBytes());
            } catch (IOException ioe) {
                logger.error("Exception while writing response", ioe);
            }
        } finally {
            if (hQuery != null) {
                hQuery.cleanUp();
            }
        }
    }
}
