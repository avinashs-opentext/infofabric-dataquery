/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */

package com.opentext.infofabric.dataquery.graphql.dataloaders;

import com.opentext.infofabric.dataquery.graphql.GraphQLService;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.HbaseBatchLoader;
import com.opentext.infofabric.dataquery.graphql.dataloaders.mock.MockBatchLoader;
import com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms.RDBMSBatchLoader;
import com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms.RDBMSSimpleLoader;
import org.dataloader.DataLoaderOptions;

public class QueryLoaderFactory {

    /**
     * @param viewType          Supported View Type enum
     * @param dataLoaderOptions Dataloader options
     * @return DataQueryLoader with appropriate query batchloader
     */
    public static DataQueryLoader getBatchLoader(GraphQLService.ViewType viewType, DataLoaderOptions dataLoaderOptions) {
        if (GraphQLService.ViewType.RDBMS.equals(viewType)) {
            return new DataQueryLoader(new RDBMSBatchLoader(), dataLoaderOptions);
        } else if (GraphQLService.ViewType.HBASE.equals(viewType)) {
            return new DataQueryLoader(new HbaseBatchLoader(), dataLoaderOptions);
        } else {
            return new DataQueryLoader(new MockBatchLoader(), dataLoaderOptions);
        }
    }

    /**
     * @param viewType          Supported View Type enum
     * @return DataQuerySimpleLoader
     */
    public static DataQuerySimpleLoader getSimpleLoader(GraphQLService.ViewType viewType) {
        if (GraphQLService.ViewType.RDBMS.equals(viewType)) {
            return new RDBMSSimpleLoader();
        }
        return null;
    }

}
