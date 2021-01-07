/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql;

import graphql.language.ArrayValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

import static com.opentext.infofabric.dataquery.DataqueryConstants.SCALAR_ANY;
import static com.opentext.infofabric.dataquery.DataqueryConstants.SCALAR_BASE64BINARY;
import static com.opentext.infofabric.dataquery.DataqueryConstants.SCALAR_DATE;
import static com.opentext.infofabric.dataquery.DataqueryConstants.SCALAR_TIME;
import static com.opentext.infofabric.dataquery.DataqueryConstants.SCALAR_TIMESTAMP;


public class CustomScalars {
    private static final Logger log = LoggerFactory.getLogger(CustomScalars.class);

    public static final GraphQLScalarType GraphQLDate = getTemporalScalarType(Date.class,
                                                        SCALAR_DATE,
                                                        "A custom graphQL Date type",
                                                        s -> new Date(Instant.parse(s).toEpochMilli()),
                                                        i -> new Date(i.toEpochMilli()),
                                                        date -> date.toString());

    public static final GraphQLScalarType GraphQLTimestamp = getTemporalScalarType( Timestamp.class,
                                                        SCALAR_TIMESTAMP,
                                                        "A custom graphQL Timestamp type",
                                                        s -> Timestamp.valueOf(LocalDateTime.parse(s)),
                                                        i -> Timestamp.valueOf(i.atZone(ZoneOffset.UTC).toLocalDateTime()),
                                                        timestamp -> timestamp.toLocalDateTime().toString());

    public static final GraphQLScalarType GraphQLTime = getTemporalScalarType(LocalTime.class,
                                                        SCALAR_TIME,
                                                        "A custom graphQL Time type",
                                                        s -> LocalTime.parse(s),
                                                        i -> i.atZone(ZoneOffset.UTC).toLocalTime(),
                                                        time -> time.toString());

    public static final GraphQLScalarType GraphQLAny =
            new GraphQLScalarType(SCALAR_ANY, "A custom graphQL Any type", new Coercing() {
                @Override
                public Object serialize(Object dataForOutput) {
                    if (dataForOutput instanceof String) {
                        return dataForOutput;
                    } else if (dataForOutput instanceof ArrayValue) {
                        return ((ArrayValue) dataForOutput);
                    } else {
                        throw new CoercingSerializeException(getErrorMessage(dataForOutput, String.class, ArrayValue.class));
                    }
                }

                @Override
                public Object parseValue(Object input) {
                    return input;
                }

                @Override
                public Object parseLiteral(Object input) {
                    try {
                        if (input instanceof ArrayValue) {
                            return (((ArrayValue) input).getValues());
                        } else {
                            return input;
                        }
                    } catch (Exception ex) {
                        throw new CoercingParseLiteralException(getErrorMessage(input, StringValue.class, IntValue.class));
                    }
                }
            });

    public static final GraphQLScalarType GraphQLBase64Binary =
            new GraphQLScalarType(SCALAR_BASE64BINARY, "A custom graphQL Base64Binary type", new Coercing() {
        @Override
        public Object serialize(Object dataForOutput) {
            if (dataForOutput instanceof String) {
                return dataForOutput;
            } else if (dataForOutput instanceof byte[]) {
                return Base64.getEncoder().encodeToString((byte[]) dataForOutput);
            } else {
                throw new CoercingSerializeException(getErrorMessage(dataForOutput, String.class, byte[].class));
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof byte[]) {
                return input;
            } else {
                try {
                    return Base64.getDecoder().decode((String) input);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new CoercingParseValueException(getErrorMessage(input, byte[].class), e);
                }
            }
        }

        @Override
        public Object parseLiteral(Object input) {
            try {
                return Base64.getDecoder().decode((StringValue.class.cast(input)).getValue());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new CoercingParseLiteralException(getErrorMessage(input, String.class), e);
            }
        }
    });

    private static String getErrorMessage(Object input, Class... allowedTypes) {
        return String.format("Expected type %s but was %s.", allowedTypes, input == null ? "null" : input.getClass().getSimpleName());
    }

    public static <T> GraphQLScalarType getTemporalScalarType(Class<T> type,
                                                              String name,
                                                              String description,
                                                              Function<String, T> parseStrFun,
                                                              Function<Instant, T> parseDateFun,
                                                              Function<T, String> serializeFun) {
        return new GraphQLScalarType(name, description, new Coercing() {

            @Override
            @SuppressWarnings("unchecked")
            public String serialize(Object dataForOutput) {
                try {
                    return serializeFun.apply((T) dataForOutput);
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    throw new CoercingSerializeException(getErrorMessage(dataForOutput, type));
                }
            }

            @Override
            public Object parseValue(Object input) {
                Object obj = null;
                try {
                    if (input instanceof String) {
                        obj = parseStrFun.apply((String) input);
                    } else if (input instanceof Long) {
                        obj = parseDateFun.apply(Instant.ofEpochMilli((Long) input));
                    } else if (type.isInstance(input)) {
                        obj = input;
                    }
                    return obj;
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    throw new CoercingParseValueException(getErrorMessage(input, String.class, Long.class));
                }
            }

            @Override
            public T parseLiteral(Object input) {
                try {
                    if (input instanceof StringValue) {
                        return parseStrFun.apply(((StringValue) input).getValue());
                    } else if (input instanceof IntValue) {
                        return parseDateFun.apply(Instant.ofEpochMilli(((IntValue) input).getValue().longValue()));
                    }
                    return null;
                } catch (Exception ex) {
                    throw new CoercingParseLiteralException(getErrorMessage(input, StringValue.class, IntValue.class));
                }
            }
        });
    }
}

