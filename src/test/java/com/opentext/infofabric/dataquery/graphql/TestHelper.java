/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentext.infofabric.dataquery.DataqueryConfiguration;
import com.opentext.infofabric.dataquery.guice.GuiceInjector;
import com.opentext.infofabric.dataquery.regression.core.TestModule;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.jackson.Jackson;

import io.dropwizard.configuration.YamlConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;

public class TestHelper {

    private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);

    public static GraphQLService GetService() {
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final YamlConfigurationFactory<DataqueryConfiguration> factory = new YamlConfigurationFactory<>(DataqueryConfiguration.class, null, objectMapper, "dw");

        final File yaml = new File(Thread.currentThread().getContextClassLoader().getResource("test-dm-dataquery.yml").getPath());
        GraphQLService graphQLService = null;
        try {
            final DataqueryConfiguration configuration = factory.build(yaml);
          //  Injector injector = Guice.createInjector(new TestModule(configuration, null));
            GuiceInjector.init(new TestModule(configuration, null));
            graphQLService = GuiceInjector.getInjector().getInstance(GraphQLService.class);

        } catch (IOException e) {
            logger.error("IOException occured while generating DataqueryConfiguration " + e.getMessage());
            Assert.fail("Could not create DataqueryConfiguration from test yaml file");
        } catch (ConfigurationException e) {
            logger.error("ConfigurationException occured while generating DataqueryConfiguration " + e.getMessage());
            Assert.fail("Could not create DataqueryConfiguration from test yaml file");
        }

        String schema =
                "directive @relationship(parentKeyFieldName: String!\n" +
                        "                childKeyFieldName: String!\n" +
                        "                bridgeName: String\n" +
                        "                parentBridgeKeyFieldName: String\n" +
                        "                childBridgeKeyFieldName: String) on FIELD_DEFINITION \n\n" +

                        "schema {" +
                        "    query: _GraphQLQuery" +
                        "}" +

                        "type _GraphQLQuery {" +
                        "    Person_query( filterset: _FilterSet! ) : [Person]" +
                        "    Person_by_id ( id : ID! ) : Person " +
                        "}" +

                        "type Person {" +
                        "    id: ID!" +
                        "    name: String" +
                        "    age: Int" +
                        "    favoriteColor: String" +
                        "    addresses(filterset: _FilterSet) : [Address] @relationship(parentKeyFieldName: \"id\" \n childKeyFieldName: \"personId\")\n" +
                        "}" +

                        "type Address {" +
                        "    id: ID!" +
                        "    personId: String" +
                        "    ownerId: String" +
                        "    street: String" +
                        "    zip: Int" +
                        "    state: String" +
                        "    owner: Person @relationship(parentKeyFieldName: \"ownerId\" \n childKeyFieldName: \"id\")\n" +
                        "}" +

                        "enum _ComparisonOperator { EQ NE LT LE GT GE LIKE IN } " +

                        "enum _LogicalOperator { AND OR } " +

                        "input _Filter {" +
                        "  field : String!" +
                        "  op : _ComparisonOperator!" +
                        "  value : String!" +
                        "  not : Boolean" +
                        "}" +

                        "input _FilterSet {" +
                        "   filtersets : [_FilterSet]" +
                        "   filters : [_Filter]" +
                        "   op : _LogicalOperator" +
                        "   not: Boolean" +
                        "}";
        graphQLService.createService("test", "person", "master", schema);
        return graphQLService;
    }
}
