/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.dataloaders;

import com.liaison.dataquery.graphql.query.Query;
import com.liaison.dataquery.graphql.results.ResultList;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;

public class DataQueryLoader extends DataLoader<Query, ResultList> {

    public DataQueryLoader(DataQueryBatchLoader batchLoadFunction, DataLoaderOptions options) {
        super(batchLoadFunction, options);
    }
}
