/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.liaison.dataquery.exception.DataqueryRuntimeException;

public class GuiceInjector {
    private static Injector injector;

    private GuiceInjector() {

    }

    public static void init(AbstractModule DataqueryModule) {
        injector = Guice.createInjector(DataqueryModule);
    }

    public static Injector getInjector() {
        if (injector == null) {
            throw new DataqueryRuntimeException("Injector requested but not yet initialized");
        }
        return injector;
    }
}