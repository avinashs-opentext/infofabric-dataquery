/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.services.impl.sdl;

import com.opentext.infofabric.dataquery.DataqueryConstants;

public class SDLCommonType {

    public static String rootQuery() {
        return String.format("schema {%1$s %2$s query: %3$s %1$s %2$s mutation: %4$s %1$s } %1$s", DataqueryConstants.LINE_BREAK, DataqueryConstants.TAB_BREAK, DataqueryConstants.QUERY_ROOT_TYPE, DataqueryConstants.MUTATION_ROOT_TYPE);
    }

    public static String customQuery() {
        StringBuilder builder = new StringBuilder()
                .append("scalar Any")
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append("enum _ComparisonOperator { EQ NE LT LE GT GE LIKE IN } ")
                .append(DataqueryConstants.DOUBLE_BREAK)

                .append("enum _LogicalOperator { AND OR } ")
                .append(DataqueryConstants.DOUBLE_BREAK)

                .append("enum _Dir {ASC DESC} ")
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append("directive @compilerDefault(class: String!) on FIELD_DEFINITION | INPUT_FIELD_DEFINITION")
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append("directive @relationship(parentKeyFieldName: String!")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("childKeyFieldName: String!")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("bridgeName: String")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("parentBridgeKeyFieldName: String")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("childBridgeKeyFieldName: String) on FIELD_DEFINITION")
                .append(DataqueryConstants.DOUBLE_BREAK)

                .append("input _Filter {")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("field : String!")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("op : _ComparisonOperator!")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("value : Any!")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("not : Boolean")
                .append(DataqueryConstants.LINE_BREAK)
                .append("}")
                .append(DataqueryConstants.DOUBLE_BREAK)

                .append("input _FilterSet {")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("filtersets : [_FilterSet]")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("filters : [_Filter]")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("op : _LogicalOperator")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("not : Boolean")
                .append(DataqueryConstants.LINE_BREAK)
                .append("}")
                .append(DataqueryConstants.DOUBLE_BREAK)

                .append("input _Sort {")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("field: String!")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("direction: _Dir")
                .append(DataqueryConstants.LINE_BREAK)
                .append("}")
                .append(DataqueryConstants.DOUBLE_BREAK)
                .append("enum _Aggregate { COUNT } ")
                ;
        return builder.toString();
    }

    public static String mutationResults() {
        StringBuilder builder = new StringBuilder()
                .append(SDLRuntimeType.SDLTypeSignature.TYPE)
                .append(" MUTATION_RESULT {")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("status: String")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("message: String")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("results: String")
                .append(DataqueryConstants.LINE_BREAK)
                .append(DataqueryConstants.TAB_BREAK)
                .append("transactionId: String")
                .append(DataqueryConstants.LINE_BREAK)
                .append("}")
                .append(DataqueryConstants.LINE_BREAK);
        return builder.toString();
    }
}