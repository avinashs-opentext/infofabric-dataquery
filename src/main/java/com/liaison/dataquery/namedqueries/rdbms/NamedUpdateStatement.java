/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.namedqueries.rdbms;

import com.liaison.dataquery.exception.DataqueryRuntimeException;
import com.liaison.dataquery.graphql.GraphQLService;
import com.liaison.dataquery.graphql.dataloaders.rdbms.RDBMSConnectionProvider;
import com.liaison.dataquery.namedqueries.NamedQuery;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class NamedUpdateStatement implements NamedQuery {

    private static final Logger logger = LoggerFactory.getLogger(NamedUpdateStatement.class);

    ExecutionResult executeQuery(String tenant, String sql) {
        final RDBMSConnectionProvider connectionProvider = RDBMSConnectionProvider.forTenant(tenant);
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = connectionProvider.getConnection();
            connection.setAutoCommit(true);
            preparedStatement = connection.prepareStatement(sql);
            int result = preparedStatement.executeUpdate();
            return new ExecutionResultImpl(result, null);
        } catch (SQLException e) {
            throw new DataqueryRuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    logger.error("Error closing prepared statement.", e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection.", e);
                }
            }
        }
    }

    @Override
    public GraphQLService.ViewType getType() {
        return GraphQLService.ViewType.RDBMS;
    }
}
