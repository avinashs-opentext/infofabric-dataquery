/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.dataloaders.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

public class HbaseConfig {

    private static final String MAPR_HBASE_DEFAULT_DB = "mapr.hbase.default.db";
    private static final String HBASE = "hbase";

    private static HbaseConfig hbaseConfigInstance = null;

    private Configuration configuration;

    private HbaseConfig() {
        configuration = HBaseConfiguration.create();
        configuration.set(MAPR_HBASE_DEFAULT_DB, HBASE);
    }

    private static HbaseConfig getInstance() {
        if (hbaseConfigInstance == null) {
            hbaseConfigInstance = new HbaseConfig();
        }
        return hbaseConfigInstance;
    }

    public static Configuration getConfiguration() {
        return getInstance().configuration;
    }

}
