/**
 * Copyright 2017 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.security;

import javax.servlet.ServletRequest;

import com.google.inject.Inject;
import com.liaison.datagate.fcl.security.DataGateSecurity;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;

import com.liaison.dataquery.DataqueryConstants;
import com.liaison.sso.dto.SSOToken;

import static com.liaison.dataquery.DataqueryConstants.SSO_ADMIN;

public class AdminLoginService implements LoginService {
    private static final String ADMIN_LOGIN_SERVICE = "Admin login service";
    protected IdentityService identityService = new DefaultIdentityService();

    private DataGateSecurity dataGateSecurity;

    @Inject
    public AdminLoginService(DataGateSecurity dataGateSecurity) {
        this.dataGateSecurity = dataGateSecurity;
    }

    @Override
    public String getName() {
        return ADMIN_LOGIN_SERVICE;
    }

    @Override
    public UserIdentity login(String username, Object credentials, ServletRequest request) {
        SSOToken token = null;

        try {
            String pass = (String) credentials;

            token = dataGateSecurity.authenticate(username, pass);
        }
        catch (Exception e) {
            // Failed to authenticate
        }

        if (token != null && dataGateSecurity.authorize(
                token.getAccessToken(),
                null,
                RestAuthorizer.RESOURCE_TYPE,
                SSO_ADMIN,
                RestAuthorizer.ADMIN_RESOURCE_CONTEXT,
                DataqueryConstants.SSO_ADMIN)) {
            return new DefaultUserIdentity(null, null, new String[]{DataqueryConstants.SSO_ADMIN});
        }

        return null;
    }

    @Override
    public boolean validate(UserIdentity user) {
        return false;
    }

    @Override
    public IdentityService getIdentityService() {
        return identityService;
    }

    @Override
    public void setIdentityService(IdentityService service) {
        this.identityService = service;
    }

    @Override
    public void logout(UserIdentity user) {
    	/**
    	 * Empty for now
    	 */
    }
}
