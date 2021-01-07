/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.regression;

import com.google.common.cache.CacheStats;
import com.liaison.datamodel.models.Model;
import com.liaison.datamodel.models.attributes.Attribute;
import com.liaison.datamodel.models.relationships.Member;
import com.liaison.datamodel.models.relationships.Relationship;
import com.liaison.datamodel.models.types.SimpleType;
import com.liaison.datamodel.models.types.Type;
import com.opentext.infofabric.dataquery.cache.ResponseCache;
import com.opentext.infofabric.dataquery.endpoints.DataQueryServlet;
import com.opentext.infofabric.dataquery.graphql.RootDataAccess;
import com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms.SQLBatchParser;
import com.opentext.infofabric.dataquery.regression.core.RegressionBase;
import com.opentext.infofabric.dataquery.regression.mock.MockModelServiceImpl;
import com.opentext.infofabric.dataquery.services.impl.SDLRelationshipsUtil;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class RDBMSRegressionTest extends RegressionBase {

    private static final Logger logger = LoggerFactory.getLogger(RDBMSRegressionTest.class);

    public static final String INTEGER = "INTEGER";

    public static EmbeddedPostgres pg = null;

    private static final String DC_UNIT_TEST_TENANT = "dc_" + UNIT_TEST_TENANT;

    @Override
    // Prepare is called before each test folder is executed
    protected void prepare(File testFiles) {
        Model model = MockModelServiceImpl.addModel(UNIT_TEST_TENANT, testFiles.getName(), getDatamodel(testFiles));
        logger.info("----- Model wired");

        try {
            if (pg != null) {
                pg.close();
            }
            pg = EmbeddedPostgres.builder().setPort(5432).start();
            Thread.sleep(1000);
            logger.info("----- Postgres started");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        createDB();
        logger.info("----- Database created");
        createSchema(testFiles);
        logger.info("----- Schema created");
        createTables(testFiles, model);
        logger.info("----- Tables created");
        insertData(testFiles, model);
        logger.info("----- Tables populated");

    }

    @Test
    public void RDBMSRegressionTests() {
        // runOnly("complex_query_01");
        runtTests("rdbms", RootDataAccess.QueryModel.MULTI_QUERY, "public");

        // basic_multi_table_01 contains two identical queries, assert that there is one cache hit
        Map<String, CacheStats> cacheStats = DataQueryServlet.getCacheStats();
        Assert.assertEquals(
                cacheStats.get(
                        ResponseCache.getCacheIdentifier(
                                UNIT_TEST_TENANT,
                                "basic_multi_table_01",
                                "rdbms")).hitCount(), 1);

        runtTests("rdbms", RootDataAccess.QueryModel.SINGLE_QUERY, "no-cache");

        cacheStats = DataQueryServlet.getCacheStats();
        Assert.assertEquals(
                cacheStats.get(
                        ResponseCache.getCacheIdentifier(
                                UNIT_TEST_TENANT,
                                "basic_multi_table_01",
                                "rdbms")).hitCount(), 1);
    }


    /*
     *
     * INITIALIZE EMBEDDED PG TABLES:
     *
     */

    private void createTables(File folder, Model model) {
        Connection connection = null;
        try {
            connection = pg.getDatabase("postgres", DC_UNIT_TEST_TENANT).getConnection();
            List<Relationship> relationships = model.getRelationships();
            Map<String, List<String>> typeToRelationshipMap = new HashMap<>();
            handleRelationships(folder, model, connection, relationships, typeToRelationshipMap);

            for (Type type : model.getTypes()) {
                List<String> columns = new ArrayList<>();
                Statement stat = connection.createStatement();
                String keyFieldName = type.getKeyFieldName();
                if (keyFieldName != null && !keyFieldName.equals("null")) {
                    columns.add(format("\"%s\" %s", keyFieldName, "INTEGER PRIMARY KEY"));
                } else {
                    columns.add("\"" + SQLBatchParser.DEFAULT_ID + "\" INTEGER PRIMARY KEY");
                }
                for (Attribute attribute : type.getAttributes()) {
                    if (!attribute.getName().equals(keyFieldName)) {
                        columns.add(format("\"%s\" %s", attribute.getName(), toPgType(attribute.getType())));
                    }
                }
                if (typeToRelationshipMap.containsKey(type.getId())) {
                    List<String> foreignKeyList = typeToRelationshipMap.get(type.getId());
                    foreignKeyList.stream().forEach(keyField -> {
                        columns.add(format("\"%s\" %s", keyField, INTEGER));
                    });

                }
                stat.executeUpdate(format("drop table if exists %s;", type.getName()));
                String create = format("create table \"%s\".\"%s\"(%s);", folder.getName(), type.getName(), String.join(", ", columns));
                stat.executeUpdate(create);
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // Ignore.
                }
            }
        }
    }

    private void handleRelationships(File folder, Model model, Connection connection, List<Relationship> relationships, Map<String, List<String>> typeToRelationshipMap) throws SQLException {
        String bridgeTableName = "";
        String parentBridgeKeyField = "";
        String childBridgeKeyField = "";

        if (relationships != null && !relationships.isEmpty()) {
            for (Relationship relationship : relationships) {
                Member member1 = relationship.getMembers().get(0);
                Member member2 = relationship.getMembers().get(1);
                if ((member1.getCardinality().getMax() == 1) && (member2.getCardinality().getMax() == -1)) {
                    populateForeignKeyFields(typeToRelationshipMap, member2);
                } else if ((member1.getCardinality().getMax() == 1) && (member2.getCardinality().getMax() == 1)) {
                    populateForeignKeyFields(typeToRelationshipMap, member1);
                    populateForeignKeyFields(typeToRelationshipMap, member2);
                } else if ((member1.getCardinality().getMax() == -1) && (member2.getCardinality().getMax() == 1)) {
                    populateForeignKeyFields(typeToRelationshipMap, member1);
                } else if ((member1.getCardinality().getMax() == -1) && (member2.getCardinality().getMax() == -1)) {
                    SDLRelationshipsUtil sdlRelationshipsUtil = new SDLRelationshipsUtil();
                    Type parentType = sdlRelationshipsUtil.getSpecificType(model.getTypes(), member1.getTypeId());
                    Type childType = sdlRelationshipsUtil.getSpecificType(model.getTypes(), member2.getTypeId());
                    parentBridgeKeyField = parentType.getName() + "_" + member1.getName() + "_id";
                    childBridgeKeyField = childType.getName() + "_" + member2.getName() + "_id";
                    bridgeTableName = sdlRelationshipsUtil.getBridgeTableName(relationship, 0, parentType, childType);
                }
            }
        }
        //bridge table creation
        if (!StringUtils.isBlank(bridgeTableName)) {
            executeBridgeTable(folder, connection, bridgeTableName, parentBridgeKeyField, childBridgeKeyField);
        }
    }

    private void executeBridgeTable(File folder, Connection connection, String bridgeTableName, String parentBridgeKeyField,
                                    String childBridgeKeyField) throws SQLException {
        Statement stat = connection.createStatement();
        List<String> columns = new ArrayList<>();
        columns.add(format("\"%s\" %s", parentBridgeKeyField, INTEGER));
        columns.add(format("\"%s\" %s", childBridgeKeyField, INTEGER));
        stat.executeUpdate(format("drop table if exists %s;", bridgeTableName));
        String create = format("create table \"%s\".\"%s\"(%s);", folder.getName(), bridgeTableName, String.join(", ", columns));
        stat.executeUpdate(create);
    }

    private void populateForeignKeyFields(Map<String, List<String>> typeToRelationshipMap, Member member) {
        List<String> foreignKeyList = new ArrayList<>();
        if (typeToRelationshipMap.containsKey(member.getTypeId())) {
            foreignKeyList = typeToRelationshipMap.get(member.getTypeId());
        }
        foreignKeyList.add(member.getKeyFieldName());
        if (!foreignKeyList.isEmpty()) {
            typeToRelationshipMap.put(member.getTypeId(), foreignKeyList);
        }
    }

    private void createDB() {
        Connection connection = null;
        try {
            connection = pg.getPostgresDatabase().getConnection();
            Statement stat = connection.createStatement();
            stat.executeUpdate("create database " + DC_UNIT_TEST_TENANT);
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // Ignore.
                }
            }
        }
    }

    private void createSchema(File folder) {
        Connection connection = null;
        try {
            connection = pg.getDatabase("postgres", DC_UNIT_TEST_TENANT).getConnection();
            Statement stat = connection.createStatement();
            stat.executeUpdate(format("create schema %s;", folder.getName()));
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // Ignore.
                }
            }
        }
    }

    private void insertData(File folder, Model model) {
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith((".csv"))) {
                String typeName = file.getName().replace(".csv", "");
                Reader in = new StringReader(getString(file));
                CSVRecord header = null;
                try {
                    int index = 0;
                    for (CSVRecord record : CSVFormat.DEFAULT.parse(in)) {
                        if (header == null) {
                            header = record;
                            continue;
                        }
                        insertRow(index++, typeName, header, record, model, folder);
                    }
                    logger.info(format("----- Inserted %d rows to %s", index, typeName));
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }
            }
        }
    }

    private void insertRow(int index, String table, CSVRecord header, CSVRecord record, Model model, File folder) {
        try {

            Connection connection = pg.getDatabase("postgres", DC_UNIT_TEST_TENANT).getConnection();

            List<String> columns = new ArrayList<>();
            List<String> values = new ArrayList<>();

            for (Type type : model.getTypes()) {
                if (type.getName().equals(table)) {
                    if (type.getKeyFieldName() == null || type.getKeyFieldName().equals("null")) {
                        columns.add(SQLBatchParser.DEFAULT_ID);
                        values.add(String.valueOf(index));
                    }
                }
            }

            for (String col : header) {
                columns.add(format("\"%s\"", col));
            }

            for (String value : record) {
                if ("NULL".equals(value)) {
                    values.add(value);
                } else {
                    values.add(format("\'%s\'", value));
                }
            }

            String insert = format("INSERT INTO \"%s\".\"%s\" (%s) values (%s) ", folder.getName(),
                    table,
                    String.join(", ", columns),
                    String.join(", ", values));

            Statement stat = connection.createStatement();
            stat.executeUpdate(insert);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            throw new RuntimeException(e);
        }
    }


    private String toPgType(SimpleType type) {
        switch (type) {
            case DOUBLE:
                return "DECIMAL";
            case FLOAT:
                return "REAL";
            case BOOLEAN:
                return "BOOLEAN";
            case SHORT:
                return "SMALLINT";
            case INTEGER:
                return INTEGER;
            case LONG:
                return INTEGER;
            case BASE64BINARY:
                return "BLOB";
            case DATE:
                return "DATE";
            case TIME:
                return "TIME";
            case DATETIME:
                return "TIMESTAMPZ";
            case CISTRING:
                return "CITEXT";
            default:
                return "TEXT";
        }
    }

    private String getDBName(File folder) {
        return "dc_" + folder.getName();
    }
}
