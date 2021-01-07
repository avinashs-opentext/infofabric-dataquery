/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.regression.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opentext.infofabric.dataquery.DataqueryConfiguration;
import com.opentext.infofabric.dataquery.dto.DataqueryRequest;
import com.opentext.infofabric.dataquery.dto.DmFilter;
import com.opentext.infofabric.dataquery.dto.DmFilterset;
import com.opentext.infofabric.dataquery.dto.DmPermission;
import com.opentext.infofabric.dataquery.dto.DmPrivileges;
import com.opentext.infofabric.dataquery.dto.DmRowPermission;
import com.opentext.infofabric.dataquery.graphql.RootDataAccess;
import com.opentext.infofabric.dataquery.regression.mock.MockRestAuthorizer;
import com.liaison.dataquery.security.ServletFilter;
import com.opentext.infofabric.dataquery.util.AppStateService;
import com.opentext.infofabric.dataquery.util.CustomizedObjectTypeAdapter;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public class RegressionBase {

    private static final Logger logger = LoggerFactory.getLogger(RegressionBase.class);
    protected static final String UNIT_TEST_TENANT = "unittesttenant";

    private CustomizedObjectTypeAdapter adapter = new CustomizedObjectTypeAdapter();
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(Map.class, adapter)
            .registerTypeAdapter(List.class, adapter)
            .create();

    private static final DropwizardTestSupport<DataqueryConfiguration> SUPPORT =
            new DropwizardTestSupport<>(TestApplication.class,
                    ResourceHelpers.resourceFilePath("test-dm-dataquery.yml"),
                    ConfigOverride.config("server.applicationConnectors[0].port", "0") // Optional, if not using a separate testing-specific configuration file, use a randomly selected port
            );

    private Client client;
    private static final String FOLDER_ROOT = "regressiontests";
    private String runOnly;

    @BeforeClass
    public void beforeClass() {
        SUPPORT.before();
        ServletFilter.setRestAuthorizer(new MockRestAuthorizer());
        ServletFilter.setUserFunction(Function.identity());
        client = new JerseyClientBuilder(SUPPORT.getEnvironment())
                .withProperty(ClientProperties.READ_TIMEOUT, 10000000)
                .build("test client");

        //Add the object security cache
        Map<String, DmPrivileges> models = new HashMap<>();
        DmPrivileges privileges = new DmPrivileges(Arrays.asList("Author", "Book"), Arrays.asList("Author", "Book"), Arrays.asList(
                new DmRowPermission("Author", Arrays.asList(
                        new DmFilterset(
                                Arrays.asList(
                                        new DmFilter("name","Hadley Ruggier")
                                )
                        )
                ))
        ));
        models.put("basic_multi_table_01", privileges);
        privileges = new DmPrivileges(Arrays.asList("Customer", "Orders"), Arrays.asList("Customer", "Orders"), Collections.emptyList());
        models.put("basic_multi_table_02", privileges);
        privileges = new DmPrivileges(Arrays.asList("Person"), Arrays.asList("Person"), Collections.emptyList());
        models.put("basic_single_table_01", privileges);
        privileges = new DmPrivileges(Arrays.asList("Employer", "Employee", "Contact", "Department"),
                Arrays.asList("Employer", "Employee", "Contact", "Department"), Collections.emptyList());
        models.put("complex_multi_table_01", privileges);
        privileges = new DmPrivileges(Arrays.asList("Employer", "Employee", "Contact", "Department"),
                Arrays.asList("Employer", "Employee", "Contact", "Department"), Collections.emptyList());
        models.put("complex_query_01", privileges);
        DmPermission permission = DmPermission.of("unittesttenant", "mock_role1", models);
        AppStateService.addPermissionsForTest("unittesttenant", "mock_role1", permission);
    }

    protected void runOnly(String folder) {
        this.runOnly = folder;
    }

    protected void prepare(File testFiles) {
        // Override in test classes
    }


    public void runtTests(String type, RootDataAccess.QueryModel queryModel, String cache) {
        File[] testFolders = getTestFolders();
        for (File folder : testFolders) {

            if (runOnly != null && !runOnly.equals(folder.getName())) {
                logger.info("----- Ignoring test suite " + folder.getName());
                continue;
            }

            logger.info("----- Preparing test suite " + folder.getName());
            prepare(folder);


            // Refresh the model for each test suite.
            Response refresh = client.target(String.format("http://localhost:%d/1/dataquery/query/graphql/%s/%s/%s?refresh=true",
                    SUPPORT.getLocalPort(), UNIT_TEST_TENANT, folder.getName(), type))
                    .request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + "encoded_token")
                    .get();
            Assert.assertEquals(refresh.getStatus(), 200, "GraphQL Schema failed to refresh.");

            List<TestQuery> queries = getQueries(folder, queryModel);
            queries.forEach(testQuery -> runTestQuery(type, folder, testQuery, queryModel, cache));

        }
    }

    private void runTestQuery(String type, File folder, TestQuery testQuery, RootDataAccess.QueryModel queryModel, String cache) {
        DataqueryRequest req = new DataqueryRequest();
        req.setQuery(testQuery.graphqlQuery);
        req.setVariables(testQuery.variables);
        String s = gson.toJson(req);
        Entity<String> json = Entity.json(s);

        Response response = client.target(String.format("http://localhost:%d/1/dataquery/query/graphql/%s/%s/%s",
                SUPPORT.getLocalPort(), UNIT_TEST_TENANT, folder.getName(), type))
                .request()
                .header("cache-control", cache) // Caches all results
                .header("x-query-model", queryModel.name())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + "encoded_token")
                .post(json);

        // Need to read the entity as string first and use customized gson to preserve integers.
        Map dataqueryApiResponse = gson.fromJson(response.readEntity(String.class), Map.class);
        logger.info(String.format("----- Asserting results for test suite %s - query %s", folder.getName(), testQuery.name));
        Assert.assertEquals(dataqueryApiResponse.get("data"), testQuery.expectedResult.get("data"));
        if (testQuery.expectedResult.containsKey("errors") && testQuery.expectedResult.get("errors") instanceof List) {
            ((List) testQuery.expectedResult.get("errors")).forEach(error -> {
                Assert.assertTrue(dataqueryApiResponse.get("errors") instanceof List);
                Assert.assertTrue(((List<String>) dataqueryApiResponse.get("errors")).stream()
                        .anyMatch((String resultError) -> String.valueOf(resultError).contains(String.valueOf(error))));
            });
        }
        logger.info("----- Success");
    }

    class TestQuery {
        String name;
        String graphqlQuery;
        Map expectedResult;
        Map variables;
    }

    private List<TestQuery> getQueries(File folder, RootDataAccess.QueryModel queryModel) {
        File[] queryFiles = folder.listFiles((dir, name) -> {
            if (name.startsWith("q") && name.endsWith(".graphql")) {
                return true;
            }
            return false;
        });
        if (queryFiles.length == 0) {
            Assert.fail("No query .graphql files in " + folder.getName());
        }
        List<TestQuery> testQueries = new ArrayList<>();
        for (File queryFile : queryFiles) {
            String name = queryFile.getName().replace(".graphql", "");
            TestQuery testQuery = new TestQuery();
            testQuery.name = name;
            String graphqlQuery = getString(queryFile);
            if (graphqlQuery.startsWith("#")) {
                String meta = graphqlQuery.substring(0, graphqlQuery.indexOf("\n"));
                if (meta.contains("QUERY_MODEL") && !meta.contains(queryModel.name())) {
                    logger.info("Skipping {}. Query model {} disabled in metadata.", name, queryModel.name());
                    continue;
                }
                graphqlQuery = graphqlQuery.substring(graphqlQuery.indexOf("\n"));
            }
            testQuery.graphqlQuery = graphqlQuery;
            File results = new File(String.format("%s/%s.json", folder.getPath(), name));
            if (!results.exists()) {
                Assert.fail(results.getName() + " does not exist in " + folder.getName());
            }
            testQuery.expectedResult = gson.fromJson(getString(results), Map.class);
            File variables = new File(String.format("%s/%s_vars.json", folder.getPath(), name));
            if (variables.exists()) {
                testQuery.variables = gson.fromJson(getString(variables), Map.class);
            }
            testQueries.add(testQuery);
        }
        return testQueries;
    }

    @AfterClass
    public void afterClass() {
        SUPPORT.after();
    }

    private File[] getTestFolders() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(FOLDER_ROOT);
        String path = url.getPath();
        return new File(path).listFiles();
    }

    protected String getDatamodel(File folder) {
        File[] files = folder.listFiles((dir, name) -> {

            if (name.equals("datamodel.json")) {
                return true;
            }
            return false;
        });
        if (files.length == 0) {
            Assert.fail("No datamodel.json in " + folder.getName());
        }
        return getString(files[0]);
    }

    protected String getString(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), "UTF-8");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }


}
