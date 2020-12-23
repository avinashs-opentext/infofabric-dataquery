/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.regression.core;

import com.google.inject.AbstractModule;
import com.liaison.dataquery.DataqueryApplication;
import com.liaison.dataquery.DataqueryConfiguration;
import io.dropwizard.setup.Environment;

public class TestApplication extends DataqueryApplication {
    @Override
    protected AbstractModule getGuiceModule(DataqueryConfiguration dataqueryConfiguration, Environment environment) {
        return new TestModule(dataqueryConfiguration, environment);
    }
}
