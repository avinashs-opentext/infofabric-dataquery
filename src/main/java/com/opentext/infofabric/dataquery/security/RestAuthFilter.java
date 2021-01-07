/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.security;

import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.dto.DataqueryApiResponse;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Priority(Priorities.AUTHENTICATION)
public class RestAuthFilter<P extends Principal> extends AuthFilter<RestCredentials, P> {

    private boolean enableSSO;

    private RestAuthFilter() {
    }

    public void setEnableSSO(boolean enableSSO) {
        this.enableSSO = enableSSO;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final String token;
        final String tenancy;
        final RestCredentials credentials;
        final Optional<P> principal;

        requestContext.getUriInfo().getAbsolutePath();

        token = cleanUpToken(requestContext.getHeaderString("Authorization"));
        tenancy = requestContext.getUriInfo().getPathParameters().getFirst(DataqueryConstants.TENANT);

        if (!enableSSO) {
            requestContext.setSecurityContext(new SecurityContext() {

                @Override
                public Principal getUserPrincipal() {
                    return new UserInfo("dummy_user", token, tenancy, "", "");
                }

                @Override
                public boolean isUserInRole(String role) {
                    return true;
                }

                @Override
                public boolean isSecure() {
                    return true;
                }

                @Override
                public String getAuthenticationScheme() {
                    return SecurityContext.BASIC_AUTH;
                }
            });
            return;
        }

        logger.trace("RestAuthFilter: tenancy = {}", tenancy);

        if (token != null) {

            String resourceContext = requestContext.getUriInfo().getPathParameters().getFirst("view").toLowerCase();
            String model = requestContext.getUriInfo().getPathParameters().getFirst("model").toLowerCase();

            String resource = null;
            if (resourceContext.equals("rdbms"))
                resource = model + "_RDBMS";
            else if (resourceContext.equals("hbase"))
                resource = model + "_HBASE";

            credentials = new RestCredentials(token, tenancy, resource, resourceContext);
            try {
                principal = authenticator.authenticate(credentials);

                if (principal.isPresent()) {
                    requestContext.setSecurityContext(new SecurityContext() {

                        @Override
                        public Principal getUserPrincipal() {
                            return principal.orElse(null);
                        }

                        @Override
                        public boolean isUserInRole(String role) {
                            return principal.filter(p -> authorizer.authorize(p, role)).isPresent();
                        }

                        @Override
                        public boolean isSecure() {
                            return requestContext.getSecurityContext().isSecure();
                        }

                        @Override
                        public String getAuthenticationScheme() {
                            return SecurityContext.BASIC_AUTH;
                        }
                    });
                    logger.trace("RestAuthFilter: Security context set successfully");
                    return;
                } else {
                    logger.warn("RestAuthFilter: Failed to set security context; no principal present");
                }
            } catch (AuthenticationException exc) {
                logger.warn("RestAuthFilter: Error authenticating credentials: " + exc.getMessage(), exc);
                throw new WebApplicationException(exc,
                        Response.status(Response.Status.UNAUTHORIZED).entity(
                                new DataqueryApiResponse()
                                        .setStatus(Response.Status.UNAUTHORIZED.name())
                                        .setMessage("Invalid credentials")).build());
            }
        } else {
            logger.warn("RestAuthFilter: Failed to set security context; no token provided");
        }
        throw
                new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity(
                        new DataqueryApiResponse()
                                .setStatus(Response.Status.UNAUTHORIZED.name())
                                .setMessage("Invalid credentials")).build());
    }


    /**
     * Builder for {@link RestAuthFilter}.
     * <p>An {@link Authenticator} must be provided during the building process.</p>
     *
     * @param <P> the principal
     */
    public static class Builder<P extends Principal> extends
            AuthFilterBuilder<RestCredentials, P, RestAuthFilter<P>> {

        @Override
        protected RestAuthFilter<P> newInstance() {
            return new RestAuthFilter<>();
        }
    }

    private static String cleanUpToken(String rawToken) {
        String token = null;
        if (StringUtils.isNotEmpty(rawToken) && rawToken.startsWith("Bearer ")) {
            token = rawToken.substring(7);
        }
        return token;
    }
}
