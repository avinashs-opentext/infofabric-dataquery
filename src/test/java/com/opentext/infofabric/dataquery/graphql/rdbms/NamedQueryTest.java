/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.rdbms;

import com.opentext.infofabric.dataquery.services.impl.NamedQueryServiceImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@Test
public class NamedQueryTest {

    @Test
    public void RecordLockGetTest() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("table", "table");
        vars.put("value", "value");
        vars.put("key", 123);
        vars.put("keyColumn", "key");
        vars.put("ttl", 60);
        vars.put("readOnly", true);
        NamedQueryServiceImpl service = new NamedQueryServiceImpl();
        Object preview = service.preview("test", "test", "pgGetRowLock", vars);
        Assert.assertTrue(preview instanceof String);
        String sql = (String) preview;
        Assert.assertTrue(sql.contains("\"lock_id\" = 'value'"));
        Assert.assertTrue(sql.contains("\"lock_read\" = 'true'"));
        Assert.assertTrue(sql.contains("\"lock_at\" = now()"));
        Assert.assertTrue(sql.contains("\"lock_exp\" = now() + interval '60'"));
        Assert.assertTrue(sql.contains("\"lock_exp\" < now()"));
    }

    @Test
    public void RecordLockReleaseTest() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("table", "table");
        vars.put("key", 123);
        vars.put("keyColumn", "key");
        vars.put("value", "value");
        NamedQueryServiceImpl service = new NamedQueryServiceImpl();
        Object preview = service.preview("test", "test", "pgReleaseRowLock", vars);
        Assert.assertTrue(preview instanceof String);
        String sql = (String) preview;
        Assert.assertTrue(sql.contains("\"lock_id\" = NULL"));
        Assert.assertTrue(sql.contains("\"lock_read\" = NULL"));
        Assert.assertTrue(sql.contains("\"lock_at\" = NULL"));
        Assert.assertTrue(sql.contains("\"lock_exp\" = NULL"));
    }
}
