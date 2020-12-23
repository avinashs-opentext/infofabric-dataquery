/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.health;

import com.codahale.metrics.health.HealthCheck;
import com.liaison.dataquery.exception.DataqueryException;

public class DataqueryHealthCheck extends BaseHealthCheck {

	@Override
	protected Result performHealthCheck() throws DataqueryException {
		return HealthCheck.Result.healthy();
	}

}
