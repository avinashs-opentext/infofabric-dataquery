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
import org.dataloader.BatchLoader;

import java.util.List;

public interface DataQueryBatchLoader extends BatchLoader<Query, ResultList> {

    void validate(List<Query> queries);

    Object getNativeQuery(List<Query> queries);

}
