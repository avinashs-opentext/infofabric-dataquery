/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;

import io.dropwizard.setup.Environment;

/**
 * Class to initialize the Dropwizard metric reporter, I think for now it can be
 * console or csv
 * 
 * @author pan
 *
 */
public final class MetricReporter {

	public static final void initReporter(DataqueryConfiguration dataqueryConfiguration, Environment environment) {
		if (dataqueryConfiguration.getMetricReporter().isEnabled()) {
			switch (dataqueryConfiguration.getMetricReporter().getType()) {
			case "csv":
				CsvReporter.forRegistry(environment.metrics()).convertRatesTo(TimeUnit.SECONDS)
						.convertDurationsTo(TimeUnit.MILLISECONDS)
						.build(new File(dataqueryConfiguration.getMetricReporter().getDirectory()))
						.start(dataqueryConfiguration.getMetricReporter().getInterval(), TimeUnit.SECONDS);
				break;
			case "console":
				ConsoleReporter.forRegistry(environment.metrics()).convertRatesTo(TimeUnit.SECONDS)
						.convertDurationsTo(TimeUnit.MILLISECONDS).build()
						.start(dataqueryConfiguration.getMetricReporter().getInterval(), TimeUnit.SECONDS);
				break;
			default:
				break;
			}
		}
	}

	private MetricReporter() {
		/**
		 * Empty For now
		 */
	}
}
