/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders;

import com.opentext.infofabric.dataquery.graphql.query.Query;
import com.opentext.infofabric.dataquery.graphql.results.ResultList;

public interface DataQuerySimpleLoader {
    ResultList load(Query query);
}
