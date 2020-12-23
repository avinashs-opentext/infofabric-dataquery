/**
 * Copyright 2019 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.dataloaders;

import com.liaison.dataquery.graphql.query.Query;
import com.liaison.dataquery.graphql.results.ResultList;

public interface DataQuerySimpleLoader {
    ResultList load(Query query);
}
