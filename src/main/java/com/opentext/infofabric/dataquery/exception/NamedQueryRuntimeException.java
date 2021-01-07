/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.exception;

public class NamedQueryRuntimeException extends RuntimeException {
    public NamedQueryRuntimeException(String msg) {
        super(msg);
    }

    public NamedQueryRuntimeException(String msg, Throwable t) {
        super(msg, t);
    }

    public NamedQueryRuntimeException(Throwable t) {
        super(t);
    }
}
