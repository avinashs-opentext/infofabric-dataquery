/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.regression.mock;

import com.opentext.infofabric.dataquery.regression.RDBMSRegressionTest;
import com.opentext.infofabric.dataquery.services.NamedQueryConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

public class MockNamedQueryConnectionProviderImpl implements NamedQueryConnectionProvider {

    @Override
    public Connection getConnection(String tenant) {
//        RDBMSConnectionProvider provider = RDBMSConnectionProvider.forTenant(tenant);
        Connection connection = null;
        try {
            connection = RDBMSRegressionTest.pg.getDatabase("postgres", "dc_" + tenant).getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
