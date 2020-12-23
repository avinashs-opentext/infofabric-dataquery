/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.services;

public interface ModelService {
    String getSDLByModel(String token, String tenant, String modelName, String branch, String version);
    String getAuthToken(String user, char[] pass);
}
