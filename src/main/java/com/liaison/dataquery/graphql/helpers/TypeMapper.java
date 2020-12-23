/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql.helpers;

import com.liaison.dataquery.exception.DataqueryRuntimeException;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.language.VariableReference;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeMapper {
    private static final String GQL_INT = "Int";
    private static final String GQL_SORT = "_Sort";
    private static final String GQL_FILTERSET = "_FilterSet";
    private static final String GQL_SORT_DIR = "direction";
    private static final String GQL_OPERATION = "op";

    private TypeMapper() {
    }

    public static Object toJavaType(Value value) {
        if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        }
        if (value instanceof BooleanValue) {
            return ((BooleanValue) value).isValue();
        }
        if (value instanceof IntValue) {
            return ((IntValue) value).getValue().longValue();
        }
        if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue().floatValue();
        }
        if (value instanceof ArrayValue) {
            List<Object> arrayResult = new ArrayList<>();
            ((ArrayValue) value).getValues().forEach(val -> arrayResult.add(toJavaType(val)));
            return arrayResult;
        }
        if (value instanceof ObjectValue) {
            Map<String, Object> mapResult = new HashMap<>();
            ((ObjectValue) value).getObjectFields().forEach(field -> mapResult.put(field.getName(), toJavaType(field.getValue())));
            return mapResult;
        }
        return value;
    }

    public static Value toGraphQLType(Object object) {
        if (object instanceof Value) {
            return (Value) object;
        }
        if (object instanceof String) {
            return new StringValue((String) object);
        }
        if (object instanceof Boolean) {
            return new BooleanValue((Boolean) object);
        }
        if (object instanceof Long) {
            return new IntValue(BigInteger.valueOf((long) object));
        }
        if (object instanceof Integer) {
            return new IntValue(BigInteger.valueOf((int) object));
        }
        if (object instanceof Double) {
            return new FloatValue(BigDecimal.valueOf((Double) object));
        }
        if (object instanceof Collection) {
            ArrayValue arrayResult = new ArrayValue();
            ((Collection) object).forEach(val -> arrayResult.getValues().add(toGraphQLType(val)));
            return arrayResult;
        }
        if (object instanceof Map) {
            ObjectValue mapResult = new ObjectValue();
            ((Map) object).forEach((key, value) -> mapResult.getObjectFields().add(new ObjectField((String) key, toGraphQLType(value))));
            return mapResult;
        }
        //TODO: Add all types.
        throw new DataqueryRuntimeException("Unsupported object type - can not convert to GraphQL type.");
    }

    public static GraphQLType getWrappedType(GraphQLOutputType type) {
        if (type instanceof GraphQLNonNull) {
            return ((GraphQLNonNull) type).getWrappedType();
        }
        if (type instanceof GraphQLList) {
            return ((GraphQLList) type).getWrappedType();
        }
        return type;
    }

    public static GraphQLType getNonNullType(GraphQLType type) {
        if (type instanceof GraphQLNonNull) {
            return ((GraphQLNonNull) type).getWrappedType();
        }
        return type;
    }

    public static void bindVariables(List<Argument> arguments, Map<String, Object> variables, GraphQLFieldDefinition fieldDefinition) {
        if (variables == null) {
            return;
        }
        for (int i = 0; i < arguments.size(); i++) {
            Argument argument = arguments.get(i);
            if (argument.getValue() instanceof VariableReference) {
                String varName = ((VariableReference) argument.getValue()).getName();
                if (!variables.containsKey(varName)) {
                    throw new DataqueryRuntimeException(String.format("Missing value for variable %s.", varName));
                }
                Object value = variables.get(varName);
                // Json does not know ints -> convert double variables to integers if defined as Int in the schema
                value = fixType(argument, value, fieldDefinition);
                Value newValue = toGraphQLType(value);
                arguments.set(i, new Argument(argument.getName(), newValue));
            }
        }
    }

    private static Object fixType(Argument argument, Object value, GraphQLFieldDefinition fieldDefinition) {
        GraphQLType nonNullType = getNonNullType(fieldDefinition.getArgument(argument.getName()).getType());
        String typeName = nonNullType.getName();
        if (typeName.equals(GQL_INT)) {
            if (value instanceof Double) {
                return ((Double) value).intValue();
            }
        } else if (typeName.equals(GQL_SORT)) {
            if (!(value instanceof Map)) {
                throw new DataqueryRuntimeException("_Sort type variable should be a json object.");
            }
            Map sortMap = (Map) value;
            toEnumValue(sortMap, GQL_SORT_DIR);
            return sortMap;
        } else if (typeName.equals(GQL_FILTERSET)) {
            if (!(value instanceof Map)) {
                throw new DataqueryRuntimeException("_FilterSet type variable should be a json object.");
            }
            Map filterMap = (Map) value;
            toEnumValue(filterMap, GQL_OPERATION);
        }
        return value;
    }

    private static void toEnumValue(Map argMap, String fieldToConvert) {
        if (argMap.containsKey(fieldToConvert)) {
            argMap.put(fieldToConvert, new EnumValue((String) argMap.get(fieldToConvert)));
        }
        argMap.forEach((key, value) -> {
            if (value instanceof Map) {
                toEnumValue((Map) value, fieldToConvert);
            } else if (value instanceof List) {
                ((List) value).forEach(el -> {
                    if (el instanceof Map) {
                        toEnumValue((Map) el, fieldToConvert);
                    }
                });
            }
        });
    }
}
