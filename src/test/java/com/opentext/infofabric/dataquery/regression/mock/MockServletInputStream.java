/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.regression.mock;


import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MockServletInputStream extends ServletInputStream {

    private final InputStream sourceStream;
    private boolean isFinished;


    public MockServletInputStream(InputStream sourceStream) {
        this.sourceStream = sourceStream;
    }

    public final InputStream getSourceStream() {
        return this.sourceStream;
    }

    public int read() throws IOException {
        int read = this.sourceStream.read();
        if (read == -1) {
            isFinished = true;
        }
        return read;
    }

    public void close() throws IOException {
        super.close();
        this.sourceStream.close();
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public boolean isReady() {
        return !isFinished;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }
}

