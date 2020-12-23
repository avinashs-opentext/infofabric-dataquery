/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery;

public class DataqueryConstants {

	public static final String API_VERSION_V1 = "1";
    public static final String API_NAME = "dataquery";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String CONTENT_DISPOSITION = "inline";
    public static final String QUERY_ROOT_TYPE = "_GraphQLQuery";
    public static final String MUTATION_ROOT_TYPE = "_GraphQLMutation";
    public static final String LINE_BREAK = "\n";
    public static final String DOUBLE_BREAK = "\n\n";
    public static final String TAB_BREAK = "\t";
    public static final String TRIPLE_TAB_BREAK = "\t\t\t";
    public static final String BY_ID_METHOD_SUFFIX = "_by_id";
    public static final String QUERY_METHOD_SUFFIX = "_query";
    public static final String SCROLL_METHOD_SUFFIX = "_scroll";
    public static final String AGGREGATE_METHOD_SUFFIX = "_aggregate";
    public static final String UPSERT_METHOD_SUFFIX = "_upsert";
    public static final String INSERT_METHOD_SUFFIX = "_insert";
    public static final String INPUT_TYPE_SUFFIX = "_dq_input_type";
    public static final String DQ_AGGREGATION_RESULT = "dq_aggregation_result";
    public static final String SSO_HEADER = "Authorization";
    public static final String PROMETHEUS_METRICS_ROOT = "dm_dataquery_";
    public static final String MUTATION_RESULT = "MUTATION_RESULT";

    public static final String RELATIONSHIP_KEY = "relationship";

    //Custom Scalar Types
    public static final String SCALAR_DATE = "Date";
    public static final String SCALAR_TIMESTAMP = "Timestamp";
    public static final String SCALAR_BASE64BINARY = "Base64binary";
    public static final String SCALAR_TIME = "Time";
    public static final String SCALAR_ANY = "Any";

    //should be in same order between path and enum types: /{{tenant}}/{{model}}/{{view}}.
    public enum PATH_PARAMS {
        TENANT, MODEL, VIEW, INFO;
        public int value() {
            return ordinal() + 1;
        }
    }

    //Resource
    public static final String TENANT = "tenant";
    public static final String SUCCESS_MESSAGE = "Success";

    //Security
    public static final String SSO_ADMIN = "admin";
    public static final String AUTH = "Authorization";
    
    public static final String SUCCESS = "success";

    public static final String FAILED = "failed";

    public static final String SEPARATOR = "/";

    public static final String API_ROOT = SEPARATOR + DataqueryConstants.API_VERSION_V1 + SEPARATOR + DataqueryConstants.API_NAME + SEPARATOR;


    // namedquery
    public static final String NAMEDQUERY_API_VERSION_V1 = "1";

    public static final String NAMEDQUERY_API_NAME = "namedquery";

    public static final String NAMEDQUERY_API_ROOT = "/" + NAMEDQUERY_API_VERSION_V1 + "/" + NAMEDQUERY_API_NAME + "/";

    public static final String COLLECTION = "task";

    public static final String DATASTREAM_PASSWORD = "password";

    public static final String DATASTREAM_SERVER = "servers";

    public static final String BOOTSTRAP_SERVER = "bootstrap.servers";

    public static final String JOB_ID_NOT_EXISTS = "Job ID not exists";

    public static final String INTERNAL_ERROR = "Internal Server Error";

    public static final int ASYNC_TIMEOUT_SECONDS = 60; // need to set 60

    public static final String NOT_VALID_COMMAND = "Not Valid Command";

    public static String STREAM_NOT_EXIST = "Stream does not exist for tenant: %s and dsid: %s.";

    public static final int PAYLOAD_RESULTSET_ROWS = 1000;

    public static final String SQL_RESOURCE = "sqlresource";
    
    public static final String DM_PERMISSION = "DM_PERMISSION";
    
    public static final String DM_MODEL = "DM_MODEL";

    //datacast configs

    public static final String DATA_PATH = "/data/";

    public static final String UNDERSCORE = "_";

    public static final String HTTP = "http";

    public static final String X_SERVICE_TOKEN = "x-service-token";

    //timeout configs

    public static final String CONNECTION_TIMEOUT = "connectionTimeout";

    public static final String SOCKET_TIMEOUT = "socketTimeout";

    public static final String CONNECTION_REQUEST_TIMEOUT = "connectionRequestTimeout";

    //status

    public static final String AUTH_HEADER = "Authorization";

    public static final String APPLICATION_JSON_CONTENT = "application/json";

    public static final String CONTENT_TYPE = "Content-Type";

	public static final String READ_PRIVILEGES = "READ_PRIVILEGES";

	public static final String WRITE_PRIVILEGES = "WRITE_PRIVILEGES";

    private DataqueryConstants() {
    	/**
    	 * Empty for now
    	 */
    }
}
