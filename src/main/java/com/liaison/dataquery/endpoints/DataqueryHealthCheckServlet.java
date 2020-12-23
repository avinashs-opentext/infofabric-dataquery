/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */

package com.liaison.dataquery.endpoints;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.liaison.dataquery.DataqueryConstants;
import com.liaison.dataquery.dto.DataqueryApiResponse;
import com.liaison.dataquery.health.BaseHealthCheck;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path(DataqueryConstants.API_ROOT + "health/")
@Api(
		value = DataqueryConstants.API_ROOT + "health/",
		tags = "Healthcheck Servlet"
)
@Produces(MediaType.APPLICATION_JSON)
public class DataqueryHealthCheckServlet extends HealthCheckServlet {

	private static final String CONTENT_TYPE = "application/json";
	private static final String RESPONSE_HEALTHY = "{\"state\":{\"healthy\":true}}";
	private static final String SIMPLE = "simple=false";

	public DataqueryHealthCheckServlet() {
    }

    public DataqueryHealthCheckServlet(HealthCheckRegistry registry) {
        super(registry);
    }

	@GET
	@ApiOperation(
			value = "Health",
			notes = "Get Healthcheck.",
			response = DataqueryApiResponse.class
	)
	@ApiResponses({
			@ApiResponse(code = 401, message = "Healthcheck fails", response = DataqueryApiResponse.class)
	})
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!SIMPLE.equals(req.getQueryString())) {
			resp.setContentType(CONTENT_TYPE);
			final OutputStream output = resp.getOutputStream();
			try {
				output.write(RESPONSE_HEALTHY.getBytes());

			} finally {
				output.close();
			}
		} else {
			super.doGet(req, resp);
		}
	}
}
