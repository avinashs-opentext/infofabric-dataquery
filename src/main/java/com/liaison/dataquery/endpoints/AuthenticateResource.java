/**
 * Copyright 2017 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.endpoints;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.liaison.datagate.fcl.security.DataGateSecurity;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.dto.DataqueryApiResponse;
import com.liaison.dataquery.dto.DataquerySSOToken;
import io.prometheus.client.Summary;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.util.Base64;

import static com.liaison.dataquery.DataqueryConstants.PROMETHEUS_METRICS_ROOT;

/**
 * Authentication endpoints
 */
@Path(DataqueryConstants.API_ROOT + AuthenticateResource.RESOURCE)
@Api(
        value = DataqueryConstants.API_ROOT + AuthenticateResource.RESOURCE,
        tags = "Authentication"
)
@Produces(MediaType.APPLICATION_JSON)
@Timed
public class AuthenticateResource  {

    private final Logger logger = LoggerFactory.getLogger(AuthenticateResource.class);
    // metrics to collect the latency
    static final Summary authLatency = Summary.build()
            .name(PROMETHEUS_METRICS_ROOT + "authenticate_resource_latency_seconds")
            .help("AuthenticateResource latency in seconds.")
            .register();
    public static final String RESOURCE = "auth";
    private DataGateSecurity dataGateSecurity;

    @Inject
    public AuthenticateResource(DataGateSecurity dataGateSecurity) {
        this.dataGateSecurity = dataGateSecurity;
    }

    @GET
    @ApiOperation(
            value = "Authenticate",
            notes = "Establishes session.",
            response = DataqueryApiResponse.class
    )
    @ApiResponses({
            @ApiResponse(code = 401, message = "If Authentication fails", response = DataqueryApiResponse.class)
    })
    public Response auth(@HeaderParam(DataqueryConstants.AUTH) String auth) {
        Summary.Timer timer = authLatency.startTimer();
        Response resp;

        try {
            logger.info("In auth service");

            if (dataGateSecurity == null) {
                resp = getUnauthorizedResponse("Invalid configuration for SSO client.");
            } else {
                String[] loginPass = new String(
                        Base64.getDecoder().decode(auth.replace("Basic ", "")),
                        Charset.forName("UTF-8"))
                        .split(":");
                String user = loginPass[0];
                String pass = loginPass[1];

                logger.trace("Invoking auth...");

                DataquerySSOToken token = new DataquerySSOToken(dataGateSecurity.authenticate(user, pass));
                token.setLoginId(user);

                resp = Response.status(Response.Status.OK).entity(
                        new DataqueryApiResponse()
                                .setStatus(Response.Status.OK.name())
                                .setMessage(DataqueryConstants.SUCCESS_MESSAGE)
                                .setResult(token)).build();
            }
        } catch (Exception exc) {
            logger.warn("Failed to authenticate; ", exc);
            resp = getUnauthorizedResponse("Failed to authenticate");
        } finally {
            timer.observeDuration();
        }

        return resp;
    }

    private Response getUnauthorizedResponse(String msg) {
        return Response.status(Response.Status.UNAUTHORIZED).entity(
                new DataqueryApiResponse()
                        .setStatus(Response.Status.UNAUTHORIZED.name())
                        .setMessage(msg)).build();
    }
}