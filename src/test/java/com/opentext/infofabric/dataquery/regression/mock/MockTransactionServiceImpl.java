/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.regression.mock;

import com.opentext.infofabric.dataquery.exception.DataCastManagerException;
import com.opentext.infofabric.dataquery.exception.MutationException;
import com.opentext.infofabric.dataquery.graphql.mutation.TransactionService;
import com.opentext.infofabric.dataquery.graphql.query.QueryContext;
import com.opentext.infofabric.dataquery.graphql.results.ResultList;
import com.opentext.infofabric.dataquery.graphql.results.ResultObject;

import java.util.HashMap;

public class MockTransactionServiceImpl implements TransactionService {
    @Override
    public ResultList upsert(QueryContext context, byte[] input) throws MutationException, DataCastManagerException {
        ResultList resultList = new ResultList();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put( "status", "200");
        hashMap.put( "message", "Success");
        ResultObject resultMap = new ResultObject(hashMap);
        resultList.add(resultMap);
        return resultList;
    }
}
