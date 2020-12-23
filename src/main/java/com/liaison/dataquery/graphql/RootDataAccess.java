/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql;

import com.liaison.dataquery.exception.DataqueryRuntimeException;
import graphql.schema.DataFetcher;
import org.dataloader.DataLoaderOptions;

import static com.liaison.dataquery.DataqueryConstants.AGGREGATE_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.BY_ID_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.QUERY_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.SCROLL_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.UPSERT_METHOD_SUFFIX;
import static com.liaison.dataquery.DataqueryConstants.INSERT_METHOD_SUFFIX;

public interface RootDataAccess extends DataFetcher {


    enum QueryType {
        BY_ID, QUERY, SCROLL, AGGREGATE, UPSERT, INSERT
    }

    enum QueryModel {
        SINGLE_QUERY, MULTI_QUERY
    }

    static QueryType resolveQueryType(String name) {
        if (name.endsWith(BY_ID_METHOD_SUFFIX)) {
            return QueryType.BY_ID;
        }
        if (name.endsWith(QUERY_METHOD_SUFFIX)) {
            return QueryType.QUERY;
        }
        if (name.endsWith(SCROLL_METHOD_SUFFIX)) {
            return QueryType.SCROLL;
        }
        if (name.endsWith(AGGREGATE_METHOD_SUFFIX)) {
            return QueryType.AGGREGATE;
        }
        if (name.endsWith(UPSERT_METHOD_SUFFIX)) {
            return QueryType.UPSERT;
        }
        if (name.endsWith(INSERT_METHOD_SUFFIX)) {
            return QueryType.INSERT;
        }
        throw new DataqueryRuntimeException("Unknown method type.");
    }

}
