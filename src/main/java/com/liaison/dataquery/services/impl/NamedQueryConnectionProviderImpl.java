/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.services.impl;

import com.liaison.dataquery.graphql.dataloaders.rdbms.RDBMSConnectionProvider;
import com.liaison.dataquery.services.NamedQueryConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

public class NamedQueryConnectionProviderImpl implements NamedQueryConnectionProvider {
    @Override
    public Connection getConnection(String tenant) {
        RDBMSConnectionProvider provider = RDBMSConnectionProvider.forTenant(tenant);
        try {
            return provider.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
