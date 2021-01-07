/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery;

import com.opentext.infofabric.registrar.types.DataCenterEnvironment;

import java.util.Map;

public class NamedQueryConfiguration {

    private static NamedQueryConfiguration config;

    private NamedQueryConfiguration(){

    }

    public static synchronized NamedQueryConfiguration getInstance(){
        if (config == null){
            config = new NamedQueryConfiguration();
        }
        return config;
    }
    private Map<String, Map<String, Object>> appState;
    private DataCenterEnvironment environment;
    private Map<String, Object> admin;
//    private String streamImplementation;
//
//    public String getStreamImplementation() {
//        return streamImplementation;
//    }
//
//    public void setStreamImplementation(String streamImplementation) {
//        this.streamImplementation = streamImplementation;
//    }

    public String getDataStreamPath() {
        return dataStreamPath;
    }

    public void setDataStreamPath(String dataStreamPath) {
        this.dataStreamPath = dataStreamPath;
    }

    private String dataStreamPath;

    public Map<String, Object> getAdmin() {
        return admin;
    }

    public void setAdmin(Map<String, Object> admin) {
        this.admin = admin;
    }

    public DataCenterEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(DataCenterEnvironment environment) {
        this.environment = environment;
//        StreamProducer.initializeEnvironment(environment);
    }

    public Map<String, Map<String, Object>> getAppState() {
        return appState;
    }

    public void setAppState(Map<String, Map<String, Object>> appState) {
        this.appState = appState;
    }

}
