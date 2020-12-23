/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.regression.mock;

import com.liaison.dataquery.graphql.GraphQLService;
import com.liaison.dataquery.graphql.dataloaders.rdbms.RDBMSBatchLoader;
import com.liaison.dataquery.graphql.dataloaders.rdbms.RDBMSConnectionProvider;
import com.liaison.dataquery.graphql.mutation.MutationInput;
import com.liaison.dataquery.graphql.mutation.TransactionService;
import com.liaison.dataquery.graphql.query.QueryContext;
import com.liaison.dataquery.graphql.results.ResultList;
import com.liaison.dataquery.regression.RDBMSRegressionTest;
import com.liaison.dataquery.services.NamedQueryConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
