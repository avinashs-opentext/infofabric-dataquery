/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.mutation;

import com.opentext.infofabric.dataquery.exception.DataCastManagerException;
import org.testng.annotations.Test;

public class TransactionServiceTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void DatacastTransactionServiceTest() throws DataCastManagerException {
        DatacastTransactionServiceImpl datacastTransactionService = new DatacastTransactionServiceImpl();
        datacastTransactionService.upsert(null, null);
    }

}
