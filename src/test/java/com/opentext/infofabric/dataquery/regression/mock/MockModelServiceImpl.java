/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.regression.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.liaison.datamodel.models.Model;
import com.opentext.infofabric.dataquery.services.impl.ModelServiceImpl;

import java.util.concurrent.ConcurrentHashMap;

public class MockModelServiceImpl extends ModelServiceImpl {

    private static final Gson gson = new GsonBuilder().create();
    private static final ConcurrentHashMap<String, String> modelJson = new ConcurrentHashMap<>();

    @Override
    protected Model getModel(String token, String tenant, String modelName, String branch, String version) {
        return getModel(tenant, modelName);
    }

    public static Model addModel(String tenant, String name, String model) {
        modelJson.put(tenant + "_" + name, model);
        return getModel(tenant, name);
    }

    public static Model getModel(String tenant, String name) {
        return gson.fromJson(modelJson.get(tenant + "_" + name), Model.class);
    }
}
