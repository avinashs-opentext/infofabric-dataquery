/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.exception;

public class DataqueryException extends Exception {
	public DataqueryException(String msg, Throwable t) {
		super(msg, t);
	}
	public DataqueryException(String msg) {
		super(msg);
	}
}