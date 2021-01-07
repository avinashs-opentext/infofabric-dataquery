/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery;

//import com.liaison.dataquery.util.DataqueryUtils;
import com.opentext.infofabric.registrar.types.DataCenterEnvironment;

import java.util.Map;

/**
 * Configuration POJO for Datastream
 */
public class DataStreamConfiguration {

    private DataCenterEnvironment environment;
    private String hosts;
    private boolean isTls = false;
    private boolean isInsecure = false;
    private String symmetricKeyPath;
    private Long socketTimeout;
    private Map<String, String> streamPath;
    private String streamPathLegacy;
    private boolean useLegacyStyream = false;
//    private String streamImplementation;

    public DataCenterEnvironment getEnvironment() {
        return environment;
    }

    public DataStreamConfiguration setEnvironment(DataCenterEnvironment environment) {
        this.environment = environment;
        return this;
    }

    public String getHosts() {
        return hosts;
    }

    public DataStreamConfiguration setHosts(String hosts) {
        this.hosts = hosts;
        return this;
    }

    public boolean isTls() {
        return isTls;
    }

    public DataStreamConfiguration setTls(boolean tls) {
        isTls = tls;
        return this;
    }

    public boolean isInsecure() {
        return isInsecure;
    }

    public DataStreamConfiguration setInsecure(boolean insecure) {
        isInsecure = insecure;
        return this;
    }

    public String getSymmetricKeyPath() {
        return symmetricKeyPath;
    }

    public DataStreamConfiguration setSymmetricKeyPath(String symmetricKeyPath) {
        this.symmetricKeyPath = symmetricKeyPath;
        return this;
    }

    public long getSocketTimeout() {
        if (socketTimeout != null) {
            return socketTimeout;
        }
        return 5000;
    }

    public DataStreamConfiguration setSocketTimeout(Long socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public String getStreamPath() {
//        if (!streamPath.containsKey(streamImplementation)) {
//            throw new RuntimeException("Missing stream path for stream implementation " + streamImplementation);
//        }
        return streamPath.get("KAFKA");
    }

    public void setStreamPath(Map<String, String> streamPath) {
        this.streamPath = streamPath;
    }

    public String getStreamPathLegacy() {
        return streamPathLegacy;
    }

    public void setStreamPathLegacy(String streamPathLegacy) {
        this.streamPathLegacy = streamPathLegacy;
    }

    public boolean isUseLegacyStyream() {
        return useLegacyStyream;
    }

    public void setUseLegacyStyream(boolean useLegacyStyream) {
        this.useLegacyStyream = useLegacyStyream;
    }

//    public String getStreamImplementation() {
//        return streamImplementation;
//    }
//
//    public void setStreamImplementation(String streamImplementation) {
//        this.streamImplementation = streamImplementation;
//    }
}
