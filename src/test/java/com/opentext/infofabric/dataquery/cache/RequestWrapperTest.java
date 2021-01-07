/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.cache;

import com.opentext.infofabric.dataquery.regression.mock.MockServletInputStream;
import com.opentext.infofabric.dataquery.security.DataqueryRequestWrapper;
import org.apache.commons.io.IOUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

public class RequestWrapperTest {

    @Test
    public void DefaultWrapperTest() throws IOException {
        InputStream targetStream = IOUtils.toInputStream("{\"query\": \"foobar\",\"variables\": {\"baz\": 1, \"buz\": 1.0}}", "UTF-8");
        MockServletInputStream mockStream = new MockServletInputStream(targetStream);
        HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedRequest.getHeader("cache-control")).thenReturn("public");
        Mockito.when(mockedRequest.getHeader("Authorization")).thenReturn("Bearer token");
        Mockito.when(mockedRequest.getPathInfo()).thenReturn("/tenant/model/view");
        Mockito.when(mockedRequest.getInputStream()).thenReturn(mockStream);

        DataqueryRequestWrapper wrapper = new DataqueryRequestWrapper(mockedRequest);

        Assert.assertTrue(wrapper.readFromCache());
        Assert.assertTrue(wrapper.writeToCache());
        Assert.assertEquals(wrapper.getCacheMapKey(), "tenant-model-view");
        Assert.assertEquals(wrapper.getCacheResultKey(), "foobarbaz=1buz=1.0");
        Assert.assertEquals(wrapper.getDataQueryRequest().getQuery(), "foobar");
        Assert.assertEquals(wrapper.getDataQueryRequest().getVariables().get("baz"), 1L);
        Assert.assertEquals(wrapper.getDataQueryRequest().getVariables().get("buz"), 1.0);
        Assert.assertEquals(wrapper.getDataQueryRequest().getTenant(), "tenant");
        Assert.assertEquals(wrapper.getDataQueryRequest().getModel(), "model");
        Assert.assertEquals(wrapper.getDataQueryRequest().getView(), "view");
        Assert.assertEquals(wrapper.getDataQueryRequest().getToken(), "token");
    }

    @Test
    public void PrivateNoStoreCacheWrapperTest() throws IOException {
        InputStream targetStream = IOUtils.toInputStream("{\"query\": \"foobar\"}", "UTF-8");
        MockServletInputStream mockStream = new MockServletInputStream(targetStream);
        HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedRequest.getHeader("cache-control")).thenReturn("private, no-store");
        Mockito.when(mockedRequest.getHeader("Authorization")).thenReturn("Bearer token");
        Mockito.when(mockedRequest.getPathInfo()).thenReturn("/tenant/model/view");
        Mockito.when(mockedRequest.getInputStream()).thenReturn(mockStream);

        DataqueryRequestWrapper wrapper = new DataqueryRequestWrapper(mockedRequest);

        Assert.assertTrue(wrapper.readFromCache());
        Assert.assertFalse(wrapper.writeToCache());
        Assert.assertEquals(wrapper.getCacheMapKey(), "tenant-model-view-token");
    }

    @Test
    public void CacheWithVariablesWrapperTest() throws IOException {
        InputStream targetStream = IOUtils.toInputStream("{\n" +
                "\"query\": \"foobar\",\n" +
                "\"variables\": {\"my_string\": \"test_string_123\", \"my_double\": 123.42323, \"my_int\": 2}\n" +
                "}", "UTF-8");
        MockServletInputStream mockStream = new MockServletInputStream(targetStream);
        HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedRequest.getHeader("cache-control")).thenReturn("public, no-cache");
        Mockito.when(mockedRequest.getPathInfo()).thenReturn("/tenant/model/view");
        Mockito.when(mockedRequest.getInputStream()).thenReturn(mockStream);

        DataqueryRequestWrapper wrapper = new DataqueryRequestWrapper(mockedRequest);

        Assert.assertFalse(wrapper.readFromCache());
        Assert.assertTrue(wrapper.writeToCache());
        Assert.assertEquals(wrapper.getCacheResultKey(), "foobarmy_string=test_string_123my_double=123.42323my_int=2");
    }

    @Test
    public void NoCacheByDefaultTest() throws IOException {
        InputStream targetStream = IOUtils.toInputStream("{\"query\": \"foobar\"}", "UTF-8");
        MockServletInputStream mockStream = new MockServletInputStream(targetStream);
        HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedRequest.getHeader("Authorization")).thenReturn("Bearer token");
        Mockito.when(mockedRequest.getPathInfo()).thenReturn("/tenant/model/view");
        Mockito.when(mockedRequest.getInputStream()).thenReturn(mockStream);

        DataqueryRequestWrapper wrapper = new DataqueryRequestWrapper(mockedRequest);

        Assert.assertFalse(wrapper.readFromCache());
        Assert.assertFalse(wrapper.writeToCache());
    }
}
