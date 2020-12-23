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
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;

import com.liaison.dataquery.DataqueryConfiguration;
import com.liaison.dataquery.DataqueryConstants;

public class AdminConstraintSecurityHandler extends ConstraintSecurityHandler {

    private static final String ADMIN_PATH = "/tasks/*";

    @Inject
    public AdminConstraintSecurityHandler(DataqueryConfiguration config, AdminLoginService loginService) {
        if (config.getDisableAdminContextAuth() == null || !config.getDisableAdminContextAuth()) {
            Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, DataqueryConstants.SSO_ADMIN);
            constraint.setAuthenticate(true);

            ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint(constraint);
            cm.setPathSpec(ADMIN_PATH);

            setAuthenticator(new BasicAuthenticator());
            addConstraintMapping(cm);
            setLoginService(loginService);
        }
    }
}

