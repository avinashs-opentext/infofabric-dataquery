/**
 * Copyright 2017 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.dto;

import com.liaison.sso.dto.SSOToken;

public class DataquerySSOToken extends SSOToken{
    private String loginId;

    public DataquerySSOToken(SSOToken token) {
        super(token.getAccessToken(), token.getRefreshToken(), token.getExpiresIn());
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }
}
