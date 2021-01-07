/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.health;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;
import com.opentext.infofabric.dataquery.exception.DataqueryException;

/**
 * Abstract Base class for Health Check.
 */
public abstract class BaseHealthCheck extends HealthCheck {

    private static final Logger log = LoggerFactory.getLogger(BaseHealthCheck.class);

    private static final int HEALTH_CHECK_TIMEOUT_MS = 30000;

    @Override
    protected Result check() throws Exception {
        try {
            return performHealthCheck();
        } catch (Exception e) {
            log.warn("HealthCheck Failed", e);
            return Result.unhealthy("HealthCheck failed, see logs for further details: " + e.getMessage());
        }
    }

    protected abstract Result performHealthCheck() throws Exception;

    protected boolean checkWithTimeout(Callable<Boolean> callable) throws InterruptedException, ExecutionException, DataqueryException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<Boolean> handler = executor.submit(callable);

        try {
            return handler.get(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            handler.cancel(true);
            throw new DataqueryException("Timeout occurred while performing HealthCheck", e);
        } finally {
            executor.shutdownNow();
        }
    }
}
