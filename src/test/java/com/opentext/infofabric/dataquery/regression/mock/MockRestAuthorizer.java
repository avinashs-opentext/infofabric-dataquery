/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.regression.mock;

import com.liaison.dataquery.security.RestAuthorizer;
import com.opentext.infofabric.dataquery.security.UserInfo;

public class MockRestAuthorizer extends RestAuthorizer {

    public MockRestAuthorizer() {
        super(null);
    }

    @Override
    public boolean authorize(UserInfo principal, String role) {
        return true;
    }

    @Override
    public boolean isTokenValid(String token) {
        return true;
    }
}
