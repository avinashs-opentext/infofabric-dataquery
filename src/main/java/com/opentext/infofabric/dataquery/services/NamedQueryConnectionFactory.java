/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.services;

import java.sql.Connection;

public class NamedQueryConnectionFactory {

    private static NamedQueryConnectionProvider namedQueryConnectionProvider = null;
    public static void setNamedQueryConnectionProvider( NamedQueryConnectionProvider provider){
        namedQueryConnectionProvider = provider;
    }
    public static Connection getConnection(String tenant){
        return namedQueryConnectionProvider.getConnection(tenant);
    }
}