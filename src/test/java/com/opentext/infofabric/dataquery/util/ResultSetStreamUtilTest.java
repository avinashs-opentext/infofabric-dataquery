/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static org.junit.Assert.assertEquals;

public class ResultSetStreamUtilTest {
    @Test
    public void DefaultWrapperTest() throws Exception {


        ResultSet rs = Mockito.mock(ResultSet.class);
        ResultSetMetaData md = Mockito.mock(ResultSetMetaData.class);


        Mockito.when(rs.getMetaData()).thenReturn(md);
        Mockito.when(md.getColumnCount()).thenReturn(2);
        Mockito.when(md.getColumnName(1)).thenReturn("id");
        Mockito.when(md.getColumnName(2)).thenReturn("name");

        Mockito.when(rs.next()).thenReturn(true, true, false);
        Mockito.when(rs.getObject("id")).thenReturn(1, 2);
        Mockito.when(rs.getObject("name")).thenReturn("first", "second");


        JSONArray arr = ResultSetStreamUtil.getSyncResponse(rs);
        assertEquals(2, arr.length());
        assertEquals("1", ((JSONObject) arr.get(0)).getString("id"));
        assertEquals("second", ((JSONObject) arr.get(1)).getString("name"));

    }
}
