/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.regression.mock;

import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.HBaseConnection;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Table;

import java.io.IOException;

public class MockHBaseConnectionImpl implements HBaseConnection {

    private static final HBaseTestingUtility utility = new HBaseTestingUtility();
    private static boolean started;

    @Override
    public void configure(Configuration c) throws IOException {
        try {
            if (!started) {
                started = true;
                utility.startMiniCluster();
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Table getTable(TableName table) throws IOException {
        return utility.getConnection().getTable(table);
    }

    @Override
    public void close() throws IOException {
        utility.getConnection().close();
    }

}
