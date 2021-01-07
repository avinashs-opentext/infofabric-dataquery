/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;

import java.io.IOException;

public class HBaseConnectionImpl implements HBaseConnection {
    private Connection dbc;

    @Override
    public void configure(Configuration c) throws IOException {
        this.dbc = ConnectionFactory.createConnection(c);
    }

    @Override
    public Table getTable(TableName table) throws IOException {
        return dbc.getTable(table);
    }

    @Override
    public void close() throws IOException {
        if (dbc != null) {
            dbc.close();
        }
    }
}
