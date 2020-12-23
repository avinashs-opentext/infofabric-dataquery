/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.mutation;

import com.liaison.dataquery.exception.DataCastManagerException;
import org.apache.commons.lang3.NotImplementedException;
import org.testng.annotations.Test;

public class TransactionServiceTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void DatacastTransactionServiceTest() throws DataCastManagerException {
        DatacastTransactionServiceImpl datacastTransactionService = new DatacastTransactionServiceImpl();
        datacastTransactionService.upsert(null, null);
    }

}
