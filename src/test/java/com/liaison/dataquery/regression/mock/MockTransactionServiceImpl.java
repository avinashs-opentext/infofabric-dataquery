/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.regression.mock;

import com.liaison.dataquery.exception.DataCastManagerException;
import com.liaison.dataquery.exception.MutationException;
import com.liaison.dataquery.graphql.mutation.TransactionService;
import com.liaison.dataquery.graphql.query.QueryContext;
import com.liaison.dataquery.graphql.results.ResultList;
import com.liaison.dataquery.graphql.results.ResultObject;

import java.util.HashMap;

public class MockTransactionServiceImpl implements TransactionService{
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
