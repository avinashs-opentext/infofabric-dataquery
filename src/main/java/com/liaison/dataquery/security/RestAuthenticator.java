/**
 * Copyright 2017 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.security;

import com.google.inject.Inject;
import com.liaison.datagate.fcl.security.DataGateSecurity;
import com.liaison.datagate.fcl.user.AuthenticateResponse;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

import java.util.Optional;


public class RestAuthenticator implements Authenticator<RestCredentials, UserInfo> {

    private DataGateSecurity dataGateSecurity;

    @Inject
    public RestAuthenticator(DataGateSecurity dataGateSecurity) {
        this.dataGateSecurity = dataGateSecurity;
    }

    @Override
    public Optional<UserInfo> authenticate(RestCredentials credentials) throws AuthenticationException {

        try {
            AuthenticateResponse authenticateResponse = dataGateSecurity.authenticate(credentials.getAccessToken());

            if (!authenticateResponse.isAuthenticated()) {
                throw new AuthenticationException("Invalid credentials");
            }

            return Optional.of(new UserInfo(authenticateResponse.getUserId(), credentials.getAccessToken(), credentials.getTenancy(), credentials.getResource(), credentials.getResourceContext()));
        } catch (Exception e) {
            throw new AuthenticationException("Invalid credentials");
        }

    }
}
