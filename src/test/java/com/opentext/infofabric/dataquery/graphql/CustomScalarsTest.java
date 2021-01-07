/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql;

import graphql.language.StringValue;
import graphql.schema.Coercing;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Date;

import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CustomScalarsTest {
    public static final String DATE_STRING = "2018-08-08T08:08:08.888Z";
    public static final String TIMESTAMP_STRING = "2018-08-08T08:08:08.888";
    public static final String TIME_STRING = "08:08:08";
    public static final String INVALID_TEST_STRING = "ADSFDFGHG";

    @BeforeClass
    public static void before() {}

    @Test
    public void testGraphQLDate() {
        Coercing coercing = CustomScalars.GraphQLDate.getCoercing();

        Object parsed = coercing.parseLiteral(new StringValue(DATE_STRING));
        Assert.assertTrue(Date.class.isInstance(parsed));

        parsed = coercing.parseValue(DATE_STRING);
        Assert.assertTrue(Date.class.isInstance(parsed));

        Assert.assertEquals(parsed.toString(), coercing.serialize(parsed));
    }

    @Test (expectedExceptions = CoercingParseLiteralException.class)
    public void testGraphQLDateWithInvalidDateStringLiteral() {
        Coercing coercing = CustomScalars.GraphQLDate.getCoercing();
        coercing.parseLiteral(new StringValue(INVALID_TEST_STRING));
    }

    @Test (expectedExceptions = CoercingParseValueException.class)
    public void testGraphQLDateWithInvalidDateStringValue() {
        Coercing coercing = CustomScalars.GraphQLDate.getCoercing();
        coercing.parseValue(INVALID_TEST_STRING);
    }

    @Test
    public void testGraphQLBase64Binary() {
        Coercing coercing = CustomScalars.GraphQLBase64Binary.getCoercing();
        byte[] base64Binary = Base64.getEncoder().encode(DATE_STRING.getBytes());
        String binaryString = new String(base64Binary);

        Object literal = coercing.parseLiteral(new StringValue(binaryString));
        Assert.assertEquals(binaryString, coercing.serialize(literal));

        Object value = coercing.parseValue(binaryString);
        Assert.assertEquals(binaryString, coercing.serialize(literal));
    }

    @Test (expectedExceptions = CoercingParseLiteralException.class)
    public void testGraphQLBase64BinaryWithInvalidBase64StringLiteral() {
        Coercing coercing = CustomScalars.GraphQLDate.getCoercing();
        coercing.parseLiteral(new StringValue(INVALID_TEST_STRING));
    }

    @Test (expectedExceptions = CoercingParseValueException.class)
    public void testGraphQLBase64BinaryWithInvalidBase64StringValue() {
        Coercing coercing = CustomScalars.GraphQLDate.getCoercing();
        coercing.parseValue(INVALID_TEST_STRING);
    }

    @Test
    public void testGraphQLTimestamp() {
        Coercing coercing = CustomScalars.GraphQLTimestamp.getCoercing();

        Object parsed = coercing.parseLiteral(new StringValue(TIMESTAMP_STRING));
        Assert.assertTrue(Timestamp.class.isInstance(parsed));

        parsed = coercing.parseValue(TIMESTAMP_STRING);
        Assert.assertTrue(Timestamp.class.isInstance(parsed));

        Assert.assertEquals(TIMESTAMP_STRING, coercing.serialize(parsed));
    }

    @Test(expectedExceptions = CoercingParseLiteralException.class)
    public void testGraphQLTimestampWithInvalidTimestampStringLiteral() {
        Coercing coercing = CustomScalars.GraphQLTimestamp.getCoercing();
        coercing.parseLiteral(new StringValue(INVALID_TEST_STRING));
    }

    @Test(expectedExceptions = CoercingParseValueException.class)
    public void testGraphQLTimestampWithInvalidTimestampStringValue() {
        Coercing coercing = CustomScalars.GraphQLTimestamp.getCoercing();
        coercing.parseValue(INVALID_TEST_STRING);
    }

    @Test
    public void testGraphQLTime() {
        Coercing coercing = CustomScalars.GraphQLTime.getCoercing();

        Object parsed = coercing.parseLiteral(new StringValue(TIME_STRING));
        Assert.assertTrue(LocalTime.class.isInstance(parsed));

        parsed = coercing.parseValue(TIME_STRING);
        Assert.assertTrue(LocalTime.class.isInstance(parsed));

        Assert.assertEquals(TIME_STRING, coercing.serialize(parsed));
    }

    @Test(expectedExceptions = CoercingParseLiteralException.class)
    public void testGraphQLTimestampWithInvalidTimeStringLiteral() {
        Coercing coercing = CustomScalars.GraphQLTime.getCoercing();
        coercing.parseLiteral(new StringValue(INVALID_TEST_STRING));
    }

    @Test(expectedExceptions = CoercingParseValueException.class)
    public void testGraphQLTimestampWithInvalidTimeStringValue() {
        Coercing coercing = CustomScalars.GraphQLTime.getCoercing();
        coercing.parseValue(INVALID_TEST_STRING);
    }
}


