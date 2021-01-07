/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.services.impl;

import com.google.inject.Inject;
import com.opentext.infofabric.common.util.JwtUtil;
import com.opentext.infofabric.datamodel.exceptions.DataModelException;
import com.opentext.infofabric.datamodel.models.Model;
import com.opentext.infofabric.datamodel.models.relationships.Relationship;
import com.opentext.infofabric.datamodel.models.types.Type;
import com.opentext.infofabric.dataquery.services.ModelService;
import com.opentext.infofabric.dataquery.services.impl.sdl.SDLCommonType;
import com.opentext.infofabric.dataquery.services.impl.sdl.SDLRelationship;
import com.opentext.infofabric.dataquery.services.impl.sdl.SDLRuntimeType;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;

import com.opentext.infofabric.datamodel.ModelClient;
import com.opentext.infofabric.dataquery.DataqueryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ModelServiceImpl implements ModelService {
    private static final Logger log = LoggerFactory.getLogger(ModelServiceImpl.class);

    @Inject
    private ModelClient modelClient;

    @Override
    public String getSDLByModel(String token, String tenant, String modelName, String branch, String version) {
        Model model = getModel(token, tenant, modelName, branch, version);
        Map<String, List<SDLRelationship>> sdlRelationshipMap = populateRelationships(model);
        StringBuilder builder = new StringBuilder()
                .append(SDLCommonType.rootQuery())
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append(SDLCommonType.customQuery())
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append(getSDLQueryTypes(model.getTypes()))
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append(getSDLMutationTypes(model.getTypes()))
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append(SDLCommonType.mutationResults())
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append(getSDLTypes(model.getTypes(), sdlRelationshipMap))
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append(getSDLMutations(model.getTypes(), sdlRelationshipMap));

        if(log.isTraceEnabled()) {
            log.trace(builder.toString());
        }
        return builder.toString();
    }

    private Map<String, List<SDLRelationship>> populateRelationships(Model model) {
        SDLRelationshipsUtil sdlRelationshipsUtil = new SDLRelationshipsUtil();
        List<Relationship> relationships = model.getRelationships();
        Map<String, List<SDLRelationship>> sdlRelationshipMap = new HashMap<>();
        if (relationships != null) {
            relationships.stream().forEach( relationship -> {
                //populate member 0
                sdlRelationshipsUtil.populateMember(model, sdlRelationshipMap, relationship, 0, 1);
                //populate member 1
                sdlRelationshipsUtil.populateMember(model, sdlRelationshipMap, relationship, 1, 0);
            });
        }
        return sdlRelationshipMap;
    }

    private String getSDLQueryTypes(List<Type> types) {
        StringBuilder builder = new StringBuilder();
        types.stream().forEach(t -> {
            SDLRuntimeType sdlType = SDLRuntimeType.instance(t, SDLRuntimeType.SDLTypeSignature.TYPE, null);
            builder.append(sdlType.typeById())
                    .append(DataqueryConstants.LINE_BREAK)
                    .append(sdlType.typeQuery())
                    .append(DataqueryConstants.LINE_BREAK)
                    .append(sdlType.typeScroll())
                    .append(DataqueryConstants.LINE_BREAK)
                    .append(sdlType.typeAggregate())
                    .append(DataqueryConstants.DOUBLE_BREAK);
        });
        return String.format("%1$s %2$s {%3$s %4$s } %3$s", SDLRuntimeType.SDLTypeSignature.TYPE, DataqueryConstants.QUERY_ROOT_TYPE, DataqueryConstants.LINE_BREAK, builder.toString());
    }

    private String getSDLMutationTypes(List<Type> types) {
        StringBuilder builder = new StringBuilder();
        types.stream().forEach(t -> {
            SDLRuntimeType sdlType = SDLRuntimeType.instance(t, SDLRuntimeType.SDLTypeSignature.INPUT, null);
            builder.append(sdlType.typeUpsert())
                    .append(DataqueryConstants.DOUBLE_BREAK);
        });
        StringBuilder builderForInput = new StringBuilder();
        types.stream().forEach(i -> {
            SDLRuntimeType sdlType = SDLRuntimeType.instance(i, SDLRuntimeType.SDLTypeSignature.INPUT, null);
            builder.append(sdlType.typeInsert())
                    .append(DataqueryConstants.DOUBLE_BREAK);
        });
        return String.format("%1$s %2$s {%3$s %4$s } %3$s", SDLRuntimeType.SDLTypeSignature.TYPE, DataqueryConstants.MUTATION_ROOT_TYPE, DataqueryConstants.LINE_BREAK, builder.toString(), builderForInput.toString());
    }

    private String getSDLTypes(List<Type> types, Map<String, List<SDLRelationship>> sdlRelationshipMap) {
        StringBuilder builder = new StringBuilder();
        types.stream().forEach(t -> {
            List<SDLRelationship> sdlRelationshipList = sdlRelationshipMap.get(t.getId());
            builder.append(SDLRuntimeType.instance(t, SDLRuntimeType.SDLTypeSignature.TYPE, sdlRelationshipList).toString())
                    .append(DataqueryConstants.DOUBLE_BREAK);
        });
        return builder.toString();
    }

    private String getSDLMutations(List<Type> types, Map<String, List<SDLRelationship>> sdlRelationshipMap) {
        StringBuilder builder = new StringBuilder();
        types.stream().forEach(t -> {
            List<SDLRelationship> sdlRelationshipList = sdlRelationshipMap.get(t.getId());
            builder.append(SDLRuntimeType.instance(t, SDLRuntimeType.SDLTypeSignature.INPUT, sdlRelationshipList).getInputTypes())
                    .append(DataqueryConstants.DOUBLE_BREAK);
        });
        return builder.toString();
    }

    protected Model getModel(String token, String tenant, String modelName, String branch, String version) {
        try {
            return  modelClient.businessDataModel(token, tenant, modelName, branch, version);
        } catch (DataModelException dme) {
            log.error(dme.getMessage(), dme);
            throw new DataqueryRuntimeException("Failed to retrieve the business model," + modelName, dme);
        }
    }
    
    @Override
    public String getAuthToken () {
        return JwtUtil.instanceOf().createAuthJWT();
//        return modelClient.authn(user, pass).getSessionToken();
    }
}
