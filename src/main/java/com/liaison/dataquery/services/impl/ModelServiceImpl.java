/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.services.impl;

import com.google.inject.Inject;
import com.liaison.datamodel.exceptions.DataModelException;
import com.liaison.datamodel.models.relationships.Relationship;
import com.liaison.dataquery.services.ModelService;
import com.liaison.dataquery.services.impl.sdl.SDLCommonType;
import com.liaison.dataquery.services.impl.sdl.SDLRelationship;
import com.liaison.dataquery.services.impl.sdl.SDLRuntimeType;
import com.liaison.dataquery.exception.DataqueryRuntimeException;

import com.liaison.dataquery.services.impl.sdl.SDLRuntimeType.SDLTypeSignature;
import com.opentext.infofabric.datamodel.ModelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liaison.dataquery.DataqueryConstants.DOUBLE_BREAK;
import static com.liaison.dataquery.DataqueryConstants.LINE_BREAK;
import static com.liaison.dataquery.DataqueryConstants.MUTATION_ROOT_TYPE;
import static com.liaison.dataquery.DataqueryConstants.QUERY_ROOT_TYPE;


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
                .append(DOUBLE_BREAK)
                .append(SDLCommonType.customQuery())
                .append(DOUBLE_BREAK)
                .append(getSDLQueryTypes(model.getTypes()))
                .append(DOUBLE_BREAK)
                .append(getSDLMutationTypes(model.getTypes()))
                .append(DOUBLE_BREAK)
                .append(SDLCommonType.mutationResults())
                .append(DOUBLE_BREAK)
                .append(getSDLTypes(model.getTypes(), sdlRelationshipMap))
                .append(DOUBLE_BREAK)
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
            SDLRuntimeType sdlType = SDLRuntimeType.instance(t, SDLTypeSignature.TYPE, null);
            builder.append(sdlType.typeById())
                    .append(LINE_BREAK)
                    .append(sdlType.typeQuery())
                    .append(LINE_BREAK)
                    .append(sdlType.typeScroll())
                    .append(LINE_BREAK)
                    .append(sdlType.typeAggregate())
                    .append(DOUBLE_BREAK);
        });
        return String.format("%1$s %2$s {%3$s %4$s } %3$s", SDLTypeSignature.TYPE, QUERY_ROOT_TYPE, LINE_BREAK, builder.toString());
    }

    private String getSDLMutationTypes(List<Type> types) {
        StringBuilder builder = new StringBuilder();
        types.stream().forEach(t -> {
            SDLRuntimeType sdlType = SDLRuntimeType.instance(t, SDLTypeSignature.INPUT, null);
            builder.append(sdlType.typeUpsert())
                    .append(DOUBLE_BREAK);
        });
        StringBuilder builderForInput = new StringBuilder();
        types.stream().forEach(i -> {
            SDLRuntimeType sdlType = SDLRuntimeType.instance(i, SDLTypeSignature.INPUT, null);
            builder.append(sdlType.typeInsert())
                    .append(DOUBLE_BREAK);
        });
        return String.format("%1$s %2$s {%3$s %4$s } %3$s", SDLTypeSignature.TYPE, MUTATION_ROOT_TYPE, LINE_BREAK, builder.toString(), builderForInput.toString());
    }

    private String getSDLTypes(List<Type> types, Map<String, List<SDLRelationship>> sdlRelationshipMap) {
        StringBuilder builder = new StringBuilder();
        types.stream().forEach(t -> {
            List<SDLRelationship> sdlRelationshipList = sdlRelationshipMap.get(t.getId());
            builder.append(SDLRuntimeType.instance(t, SDLTypeSignature.TYPE, sdlRelationshipList).toString())
                    .append(DOUBLE_BREAK);
        });
        return builder.toString();
    }

    private String getSDLMutations(List<Type> types, Map<String, List<SDLRelationship>> sdlRelationshipMap) {
        StringBuilder builder = new StringBuilder();
        types.stream().forEach(t -> {
            List<SDLRelationship> sdlRelationshipList = sdlRelationshipMap.get(t.getId());
            builder.append(SDLRuntimeType.instance(t, SDLTypeSignature.INPUT, sdlRelationshipList).getInputTypes())
                    .append(DOUBLE_BREAK);
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
    public String getAuthToken (String user, char[] pass) throws DataModelException {
        return modelClient.authn(user, pass).getSessionToken();
    }
}
