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
import com.opentext.infofabric.dataquery.dto.NamedQueryApiResponse;
import com.opentext.infofabric.dataquery.dto.SqlResource;
import com.opentext.infofabric.dataquery.util.AppStateService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(DataqueryConstants.API_ROOT)


public class NamedQuerySqlResource {

    @POST
    @RolesAllowed({"QUERY"})
    @Path("{tenant}/{model}/{view}/updateSql/{fileName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response updateSql(
            @PathParam("tenant") final String tenant, @PathParam("model") final String model, @PathParam("view") final String view,
            @PathParam("fileName") final String fileName, byte[] inputFile) {

        SqlResource res = new SqlResource();
        res.setFileName(fileName);
        res.setFileContent(new String(inputFile));
        AppStateService.writeSql(fileName,res);
        return  Response.ok(new NamedQueryApiResponse().setStatus(DataqueryConstants.SUCCESS).setResult("success")).build();


    }
}
