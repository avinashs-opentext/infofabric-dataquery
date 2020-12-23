/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.services.impl.sdl;

import static com.liaison.dataquery.DataqueryConstants.DOUBLE_BREAK;
import static com.liaison.dataquery.DataqueryConstants.LINE_BREAK;
import static com.liaison.dataquery.DataqueryConstants.MUTATION_ROOT_TYPE;
import static com.liaison.dataquery.DataqueryConstants.QUERY_ROOT_TYPE;
import static com.liaison.dataquery.DataqueryConstants.TAB_BREAK;

public class SDLCommonType {

    public static String rootQuery() {
        return String.format("schema {%1$s %2$s query: %3$s %1$s %2$s mutation: %4$s %1$s } %1$s", LINE_BREAK, TAB_BREAK, QUERY_ROOT_TYPE, MUTATION_ROOT_TYPE);
    }

    public static String customQuery() {
        StringBuilder builder = new StringBuilder()
                .append("scalar Any")
                .append(DOUBLE_BREAK)
                .append("enum _ComparisonOperator { EQ NE LT LE GT GE LIKE IN } ")
                .append(DOUBLE_BREAK)

                .append("enum _LogicalOperator { AND OR } ")
                .append(DOUBLE_BREAK)

                .append("enum _Dir {ASC DESC} ")
                .append(DOUBLE_BREAK)
                .append("directive @compilerDefault(class: String!) on FIELD_DEFINITION | INPUT_FIELD_DEFINITION")
                .append(DOUBLE_BREAK)
                .append("directive @relationship(parentKeyFieldName: String!")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("childKeyFieldName: String!")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("bridgeName: String")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("parentBridgeKeyFieldName: String")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("childBridgeKeyFieldName: String) on FIELD_DEFINITION")
                .append(DOUBLE_BREAK)

                .append("input _Filter {")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("field : String!")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("op : _ComparisonOperator!")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("value : Any!")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("not : Boolean")
                .append(LINE_BREAK)
                .append("}")
                .append(DOUBLE_BREAK)

                .append("input _FilterSet {")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("filtersets : [_FilterSet]")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("filters : [_Filter]")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("op : _LogicalOperator")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("not : Boolean")
                .append(LINE_BREAK)
                .append("}")
                .append(DOUBLE_BREAK)

                .append("input _Sort {")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("field: String!")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("direction: _Dir")
                .append(LINE_BREAK)
                .append("}")
                .append(DOUBLE_BREAK)
                .append("enum _Aggregate { COUNT } ")
                ;
        return builder.toString();
    }

    public static String mutationResults() {
        StringBuilder builder = new StringBuilder()
                .append(SDLRuntimeType.SDLTypeSignature.TYPE)
                .append(" MUTATION_RESULT {")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("status: String")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("message: String")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("results: String")
                .append(LINE_BREAK)
                .append(TAB_BREAK)
                .append("transactionId: String")
                .append(LINE_BREAK)
                .append("}")
                .append(LINE_BREAK);
        return builder.toString();
    }
}