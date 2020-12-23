/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.graphql;

import com.liaison.dataquery.DataqueryConstants;
import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.FieldComplexityEnvironment;
import graphql.execution.AbortExecutionException;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vkukkadapu on 11/19/18.
 */
public class FieldComplexityCalculatorImpl implements FieldComplexityCalculator {

    private final static Logger logger = LoggerFactory.getLogger(FieldComplexityCalculatorImpl.class);

    private static final String SCHEMA_INTROSPECTION = "__Schema";

    private int maxJoinFieldComplexity;
    private int maxNormalFieldComplexity;

    public FieldComplexityCalculatorImpl(int maxJoinFieldComplexity, int maxNormalFieldComplexity) {
        this.maxJoinFieldComplexity = maxJoinFieldComplexity;
        this.maxNormalFieldComplexity = maxNormalFieldComplexity;
    }

    @Override
    public int calculate(FieldComplexityEnvironment environment, int childComplexity) {
        GraphQLFieldDefinition fieldSDL = environment.getFieldDefinition();
        GraphQLDirective relationshipDirective = fieldSDL.getDirective(DataqueryConstants.RELATIONSHIP_KEY);
        if (environment.getParentEnvironment() != null &&
                environment.getParentEnvironment().getParentType() != null &&
                SCHEMA_INTROSPECTION.equals(environment.getParentEnvironment().getParentType().getName())) {
            // Reduce complexity to allow introspection queries
            childComplexity = childComplexity - 1000000;
        } else if (relationshipDirective != null) {
            // if the field is join field increment by 1000
            childComplexity = childComplexity + 1000;
        } else {
            // if the field is normal field increment by 1
            childComplexity = childComplexity + 1;
        }
        if ((childComplexity % 1000) > maxNormalFieldComplexity) {
            logger.error("Number of selected fields in the Query {} exceeded max of {} selected fields", (childComplexity % 1000), maxNormalFieldComplexity);
            throw new AbortExecutionException(String.format("Number of selected fields in the Query { %s } exceeded max of { %s } selected fields", (childComplexity % 1000), maxNormalFieldComplexity));
        } else if ((childComplexity / 1000) > maxJoinFieldComplexity) {
            logger.error("Number of join fields in the Query {} exceeded max of {} join fields allowed", (childComplexity / 1000), maxJoinFieldComplexity);
            throw new AbortExecutionException(String.format("Number of join fields in the Query { %s } exceeded max of { %s } join fields allowed", (childComplexity / 1000), maxJoinFieldComplexity));
        }
        // for a normal field increment by 1
        return childComplexity;
    }

}
