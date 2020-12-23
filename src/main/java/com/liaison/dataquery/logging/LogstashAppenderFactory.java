/**
 * Copyright 2017 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.logging;

import java.util.Map;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liaison.dataquery.exception.DataqueryRuntimeException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.async.AsyncLoggingEventAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.fieldnames.LogstashFieldNames;

/**
 * Appender factory to create LogstashTcpSocketAppender.
 */
@JsonTypeName("logstash")
public class LogstashAppenderFactory extends AbstractAppenderFactory {
    @NotNull
    private String host;

    @Min(1)
    @Max(65535)
    private int port;

    @Min(1)
    @Max(65535)
    private int queueSize = LogstashTcpSocketAppender.DEFAULT_QUEUE_SIZE;

    private Map<String, String> properties;

    @JsonProperty
    public void setHost(String host) {
        this.host = host;
    }

    @JsonProperty
    public String getHost() {
        return host;
    }

    @JsonProperty
    public void setPort(int port) {
        this.port = port;
    }

    @JsonProperty
    public int getPort() {
        return port;
    }

    @Override
    @JsonProperty
    public int getQueueSize() {
        return queueSize;
    }

    @Override
    @JsonProperty
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    @JsonProperty
    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonProperty
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    private LogstashFieldNames getFieldNames() {
        LogstashFieldNames logstashFieldNames = new LogstashFieldNames();
        logstashFieldNames.setLogger("logger");
        logstashFieldNames.setThread("thread");
        logstashFieldNames.setLevelValue(null);
        return logstashFieldNames;
    }

    private String checkPropertyValue(String value) {
        if (value == null) {
            return null;
        } else if (value.matches("\\$\\{sys:.*\\}")) {
            return System.getProperty(value.substring(6, value.length() - 1));
        } else if (value.matches("\\$\\{env:.*\\}")) {
            return System.getenv(value.substring(6, value.length() - 1));
        }
        return value;
    }

    @Override
    public Appender build(LoggerContext context, String applicationName, LayoutFactory layoutFactory, LevelFilterFactory levelFilterFactory, AsyncAppenderFactory asyncAppenderFactory) {
        final LogstashTcpSocketAppender appender = new LogstashTcpSocketAppender();
        final LogstashEncoder encoder = new LogstashEncoder();

        appender.setName("logstash-appender");
        appender.setContext(context);
        appender.addDestination(host + ":" + port);
        appender.setQueueSize(queueSize);

        if (properties != null) {
            properties.forEach((k, v) -> properties.put(k, checkPropertyValue(v)));
            properties.put("java_version", System.getProperty("java.version"));

            try {
                encoder.setCustomFields(new ObjectMapper().writeValueAsString(properties));
            } catch (JsonProcessingException e) {
                throw new DataqueryRuntimeException("Unable process Logstash appender properties.", e);
            }
        }

        encoder.setFieldNames(getFieldNames());
        appender.setEncoder(encoder);
        super.setThreshold(threshold.toString());
        encoder.start();
        appender.start();

        return wrapAsync(appender, new AsyncLoggingEventAppenderFactory());
    }
}
