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
import com.google.gson.JsonObject;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.Version;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(VersionResource.API_ROOT + VersionResource.RESOURCE)
@Api(
        value = VersionResource.API_ROOT + VersionResource.RESOURCE,
        tags = "Version"
)
public class VersionResource {

    // URL endpoints
    public static final String API_ROOT = "/" + DataqueryConstants.API_VERSION_V1 + "/" + DataqueryConstants.API_NAME + "/";
    public static final String RESOURCE = "version";
    public static final String PRETTY = "pretty";

    @GET
    @ApiOperation(
            value = "Get version and commit",
            notes = "Returns JSON version and commit info",
            response = Response.class
    )

    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersion(
            @ApiParam(value = "Pretty print") @QueryParam(PRETTY) boolean pretty) {
        JsonObject response = new JsonObject();
        response.addProperty(RESOURCE, Version.VERSION);
        response.addProperty("commit", Version.COMMIT);
        Gson gson = pretty ? new GsonBuilder().setPrettyPrinting().create() : new Gson();
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(gson.toJson(response)).build();
    }
}