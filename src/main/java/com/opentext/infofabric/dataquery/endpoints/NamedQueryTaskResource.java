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
import com.opentext.infofabric.dataquery.guice.GuiceInjector;
import com.opentext.infofabric.dataquery.dto.NamedQueryApiResponse;
import com.opentext.infofabric.dataquery.dto.NamedQueryRequest;
import com.opentext.infofabric.dataquery.dto.NamedQueryResponse;
import com.opentext.infofabric.dataquery.exception.NamedQueryRuntimeException;
import com.opentext.infofabric.dataquery.services.NamedQueryExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Path(DataqueryConstants.NAMEDQUERY_API_ROOT)


public class NamedQueryTaskResource {
    private static final Logger log = LoggerFactory.getLogger(NamedQueryTaskResource.class);
    private static NamedQueryExecutorService namedQueryExecutorService= null;

    private static final String SUCCESS = "success";
    private static final String FAILED = "failed";

    public NamedQueryTaskResource(){
        namedQueryExecutorService = GuiceInjector.getInjector().getInstance(NamedQueryExecutorService.class);
    }

    @POST
    @RolesAllowed({"QUERY"})
    @Path("{tenant}/{model}/{view}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void executeCommand(@PathParam("tenant") final String tenant, @PathParam("model") final String model,
                               @PathParam("view") final String view,
                               final NamedQueryRequest dqr, final @Suspended AsyncResponse response) {


                try {
                    String operation = dqr.getCommand();
                    log.info("received request for operation "+operation);
                    String inputQuery = dqr.getQuery();
                    Map<String, Object> info = new HashMap<>();
                    if (operation != null) {

                        switch (operation) {
                            case "preview":
                                String query = namedQueryExecutorService.previewQuery(tenant, model, dqr);
                                response.resume(Response.ok(new NamedQueryApiResponse().
                                        setStatus(DataqueryConstants.SUCCESS).setResult(query)).build());
                                break;

                            case "list":
                                Set<String> result = namedQueryExecutorService.getNames();
                                response.resume(Response.ok(new NamedQueryApiResponse().
                                        setStatus(DataqueryConstants.SUCCESS).setResult(result)).build());
                                break;

                            case "view":
                                String resultQuery = namedQueryExecutorService.getQuery(inputQuery);
                                response.resume( Response.ok(new NamedQueryApiResponse().
                                        setStatus(DataqueryConstants.SUCCESS).setResult(resultQuery)).build());
                                break;

                            case "execute":
                                namedQueryExecutorService.executeQuery(tenant, model, dqr, response);
                                break;

                            case "status":
                                NamedQueryResponse output = namedQueryExecutorService.getJobIdStatus(dqr.getJobId());
                                Response resp = null;
                                if(output == null){
                                    resp= Response.ok(new NamedQueryApiResponse().
                                            setStatus(DataqueryConstants.FAILED).setMessage(DataqueryConstants.JOB_ID_NOT_EXISTS)).build();

                                } else {
                                    resp =  Response.ok(new NamedQueryApiResponse().setStatus(DataqueryConstants.SUCCESS).setResult(output)).build();
                                }
                                response.resume(resp);
                                break;

                                default:
                                response.resume( Response.ok(new NamedQueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(DataqueryConstants.NOT_VALID_COMMAND)).build());
                                    break;

                        }
                    }
                } catch (NamedQueryRuntimeException dre) {
                    log.error(dre.getMessage(), dre);
                    response.resume( Response.ok(new NamedQueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(dre.getMessage())).build());

               } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    response.resume( Response.ok(new NamedQueryApiResponse().setStatus(DataqueryConstants.FAILED).setMessage(DataqueryConstants.INTERNAL_ERROR)).build());

                }
            }


}

