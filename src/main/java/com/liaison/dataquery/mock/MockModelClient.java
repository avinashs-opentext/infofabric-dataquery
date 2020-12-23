/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.mock;

import com.liaison.datamodel.DMLOperation;
import com.liaison.datamodel.ModelClient;
import com.liaison.datamodel.auth.SSOToken;
import com.liaison.datamodel.compiler.IViewCompiler;
import com.liaison.datamodel.compiler.ViewType;
import com.liaison.datamodel.exceptions.DataModelException;
import com.liaison.datamodel.exceptions.InvalidTokenException;
import com.liaison.datamodel.exceptions.ModelConflictException;
import com.liaison.datamodel.exceptions.UnauthorizedUserException;
import com.liaison.datamodel.models.Model;
import com.liaison.datamodel.models.Namespace;
import com.liaison.datamodel.models.attributes.Attribute;
import com.liaison.datamodel.models.references.Reference;
import com.liaison.datamodel.models.relationships.Relationship;
import com.liaison.datamodel.models.types.Type;
import com.liaison.registrar.types.Payload;
import com.opentext.infofabric.datamodel.ModelClient;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MockModelClient implements ModelClient {
    private static final String NOT_IMPLEMENTED = "Method not implemented for mock client.";
    private static final List<Model> models = Arrays.asList(
            new Model("testmodel", null, null),
            new Model("c1_model", null, null),
            new Model("c2_model", null, null),
            new Model("c1_model_b", null, null));

    @Override
    public Boolean modelExists(String tenant, String modelName) {
        return models.stream().anyMatch(m -> m.getName().equals(modelName));
    }

    @Override
    public Boolean datamodelExists(String token, String tenant, String modelName, String branch, String version) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public List<Model> listModels(String token, String tenant) throws DataModelException {
        return models;
    }

    @Override
    public String createRelationship(String token, String tenant, Relationship relationship, String modelid, String branch) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void setRelationship(String token, String tenant, String relationshipID, String modelId, String branch, Relationship relationship) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Boolean isServiceHealthy() throws DataModelException {
        return null;
    }

    @Override
    public SSOToken authn(String s, char[] chars) throws UnauthorizedUserException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public IViewCompiler getCompiler(String tenant, String modelName, ViewType type) {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public IViewCompiler getDatamodelCompiler(String s, String s1, String s2, String s3, String s4, ViewType viewType, Map map) throws DataModelException, InvalidTokenException {
        return null;
    }

    @Override
    public List<String> getDDL(Model model, ViewType viewType, Map map) throws DataModelException {
        return null;
    }

    @Override
    public List<String> getDML(Model model, ViewType viewType, Map map, Payload payload, DMLOperation dmlOperation) throws DataModelException {
        return null;
    }

    @Override
    public <T> List<T> updateDDL(String s, String s1, String s2, String s3, String s4, String s5, String s6, ViewType viewType, Map map) throws DataModelException, InvalidTokenException, ModelConflictException {
        return null;
    }

    @Override
    public Model businessModel(String tenant, String modelName) {
        return models.get(0);
    }

    @Override
    public Model businessDataModel(String token, String tenant, String modelName, String branch, String version) throws DataModelException {
        return models.get(0);
    }

    @Override
    public void createTenant(String token, String tenant) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void deleteTenant(String token, String tenant) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public List<String> listTenants(String token) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public String createAttribute(String token, String tenant, Attribute attribute, String modelId, String typeid, String branch) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void setAttribute(String token, String tenant, String modelid, String typeid, String attributeID,  Attribute attribute, String branch) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Attribute readAttribute(String token, String tenant, String attributeID) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Attribute readAttributeById(String token, String tenant, String modelid, String typeid, String attributeid, String branch, String version) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void deleteAttribute(String s, String s1, String s2, String s3, String s4, String s5) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public String createType(String token, String tenant, Type type, String modelId, String branch) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void setType(String token, String tenant, String typeID,  String modelId, String branch, Type type) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Type readType(String token, String tenant, String typeID) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Type readTypeById(String token, String tenant, String typeID , String modelId, String branch, String version) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void deleteType(String token, String tenant, String typeID, String modelId, String branch) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public String createModel(String token, String tenant, Model model) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void setModel(String token, String tenant, String modelID, Model model, String branch) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Model readModel(String token, String tenant, String modelID) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Model readModelById(String token, String tenant, String modelID, String branch, String version) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void deleteModel(String token, String tenant, String modelID) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Relationship readRelationship(String token, String tenant, String relationshipID) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Relationship readRelationshipById(String token, String tenant, String relationshipID , String modelId, String branch, String version) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void deleteRelationship(String token, String tenant, String relationshipID, String modelId, String branch) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public List<Namespace> listNamespaces(String token, String tenant) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public List<Model> listModelsForNamespace(String token, String tenant, String uri) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public String createReference(String token, String tenant, Reference reference, String modelid, String branch) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void setReference(String token, String tenant, String referenceID, String modelId, String branch, Reference reference) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Reference readReference(String token, String tenant, String referenceID) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public Reference readReferenceById(String token, String tenant, String referenceID,String modelId, String branch, String version) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void deleteReference(String token, String tenant, String referenceID, String modelId, String branch) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void createBranch(String token, String tenant, String modelId, String targetBranch, String sourceBranch, String sourceVersion) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void commitBranch(String token, String tenant, String modelId, String branch, String message) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public void mergeBranch(String token, String tenant, String modelId, String branch, String message, boolean deleteBranchFlag) throws DataModelException, InvalidTokenException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public List<String> listBranchNamesByModelId(String token, String tenant, String modelId) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public List<String> listBranchNamesByModelName(String token, String tenant, String modelName) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public List<String> listVersionsByModelId(String token, String tenant, String modelId) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }

    @Override
    public List<String> listVersionsByModelName(String token, String tenant, String modelName) throws DataModelException {
        throw new NotImplementedException(NOT_IMPLEMENTED);
    }
}
