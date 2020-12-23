/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.dataloaders.mock;

import com.liaison.dataquery.exception.QueryValidationException;
import com.liaison.dataquery.graphql.dataloaders.DataQueryBatchLoader;
import com.liaison.dataquery.graphql.query.Query;
import com.liaison.dataquery.graphql.results.ResultList;
import com.liaison.dataquery.graphql.results.ResultObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MockBatchLoader implements DataQueryBatchLoader {

    @Override
    public CompletionStage<List<ResultList>> load(List<Query> keys) {
        return CompletableFuture.supplyAsync(() -> {
            List<ResultList> results = new ArrayList<>();
            for (Query q : keys) {
                int size = 10;
                if(q.getFirstJoinCondition() != null && !q.getFirstJoinCondition().hasMany()){
                    size = 1;
                }
                results.add(getMany(q, size));
            }
            return results;
        });
    }

    @Override
    public void validate(List<Query> queryKey) {
        if (queryKey == null) {
            throw new QueryValidationException("Missing query key.");
        }
        if (queryKey.get(0).getProjectionSet() == null) {
            throw new QueryValidationException("Missing projections.");
        }
        if (queryKey.get(0).getCollection() == null) {
            throw new QueryValidationException("Missing collection.");
        }
    }

    @Override
    public Object getNativeQuery(List<Query> queryKey) {
        final Map<String, Object> result = new HashMap<>();
        queryKey.get(0).getProjectionSet().getProjections().forEach(s -> result.put(s.getField(), "-> mock"));
        return result;
    }


    public static ResultList getMany(Query queryKey, int size) {
        ResultList resultSet = IntStream.range(0, size).mapToObj(i -> mockData(queryKey, i)).collect(Collectors.toCollection(ResultList::new));
        return resultSet;
    }

    public static ResultObject mockData(Query queryKey, int id) {
        final ResultObject result = new ResultObject();
        queryKey.getProjectionSet().getProjections().forEach(s -> {
            switch (s.getDataType()) {
                case "String":
                    result.put(s.getField(), s.getDataType() + " for " + queryKey.getCollection().getName() + "." + s.getField());
                    break;
                case "Int":
                    result.put(s.getField(), 123);
                    break;
                case "Float":
                    result.put(s.getField(), 4.2);
                    break;
                case "ID":
                    result.put(s.getField(), "ID" + id);
                    break;
                default:
                    result.put(s.getField(), null);
                    break;
            }
        });
        return result;
    }
}
