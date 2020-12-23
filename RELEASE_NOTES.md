1.0.15
==============
* Object Security Implementation
* Epic link https://jira.opentext.com/browse/ALDM-126

1.0.14
==============
* Update configs for minikube 

1.0.13
==============
* Cleaned up unnecessary config maps and secrets. Fixed minikube k8s configs

1.0.12
==============
* Rethrow exceptions thrown in Data loader and make available to the user

1.0.11
==============
* Opt-in response cache
* Integer deserialization fixes
* Move request logging to servlet filter

1.0.10
==============
* Added logging in the endpoints to print calling user and IP of the caller

1.0.9
==============
* Add Hikari connection pool minimumIdle to 0 and idleTimeout to 60000 Ms

1.0.8
==============
* Added implementation to limit the number of selected fields and number of join fields in the GraphQL query
  based on config value.

1.0.7
==============
* Added IN operator implementation and regression tests for that

1.0.6
==============
* Log the name of the property when the code fails to decrypt
* Cleanup configs - remove maprAdminClientConfig and rename admin config as maprAdminClientConfig as both have same 
  values

1.0.5
==============
* K8sfile is modified to reference the config maps and secrets from 
  alloy-dm-common and alloy-dm-common-secrets files

1.0.4
==============
* Modify jenkins pipeline to add slack approval for UAT deployment

1.0.3
==============
* Remove redundant symmetric keys in K8ssfile

1.0.2
==============
* Mutation root fetcher implementation
* Mutation RDBMS test support

1.0.1
==============
* remove Staging deployment

1.0.0
==============
* Add implementation to many to many relationships and add regression tests

0.1.80
==============
* Fixes to GraphQL relationships implementation

0.1.79
==============
* Update JaCoCo version

0.1.78
==============
* Set the database type in the job configuration to hbase

0.1.77
==============
* Fix possible nullpointer when relationships are not existing

0.1.76
==============
* Added relationships in getSDL endpoint

0.1.75
==============
* Refactor hbase config and properly close the resources to tune the performance of hbase queries

0.1.74
==============
* update jre to registry-master

0.1.73
==============
* Add read timeout for the zookeeper server connection check

0.1.72
==============
* update jre to 1.8

0.1.71
======
* Update Secret for Mapr

0.1.70
==============
* Rollback JVM parameters as K8 paniked

0.1.69
==============
* Adjust JVM startup parameters to tune Garbage Collection for K8

0.1.68
==============
* Mutation SDL
* Mutation implementation stubbing

0.1.67
==============
* Update SSO Client Id

0.1.66
==============
* Add metrics for HBASE query request and clean up existing ones for rdbms

0.1.65
==============
* Fix the test as at times it was failing with starting database error

0.1.64
==============
* Added regression tests for Time data type

0.1.63
==============
* Fix serialization issue with java.sql.Time field when it is present in selection list in the query
* Fix the issue when java.sql.Time is present in filter of the query

0.1.62
==============
* Remove E2E report from jenkins file for now

0.1.61
==============
* Replace SQLite with embedded Postgresql in regression tests

0.1.60
==============
* Adjust Kubernetes memory size back to default 2Gi

0.1.59
==============
* Add QA and staging Deployment

0.1.58
==============
* Add JAVA_OPTS for GC and Memory

0.1.57
==============
* Adjust jvm max/min parameter for K8 environment

0.1.56
==============
* Add startup parameter to define Java max and min memory for dataquery application

0.1.55
==============
* Fix issue with citext field returning case sensitive strings instead if case insensitive strings

0.1.54
==============
* Fix serialization issue with date field when it is present in selection list in the query

0.1.53
==============
* Add SsoEnable property to Kubernetes config map

0.1.52
==============
* Adjust size for ssoClient configuration parameters for cacheSize and cacheTtl

0.1.51
==============
* Adjust metrics collection to time getConnection method

0.1.50
==============
* Cleanup error messages that we return when validations fail. Make the error messages user friendly and informative.

0.1.49
==============
* Remove minimumIdle setting and update initializationFailTimeout to 100

0.1.48
==============
* Fix the issue with executing SQL query when GraphQL field type is Timestamp in the filter condition

0.1.47
==============
* Fix the issue with executing SQL query when GraphQL field type is Float in the filter condition

0.1.46
==============
* Add metrics for dataloader cache hit and miss ratio

0.1.45
==============
* Added a Gradle plugin to generate third-party lib license report

0.1.44
==============
* Implemented GraphQL aggregate query

0.1.43
==============
* Add performance metrics on auth, prepare and execute query

0.1.42
==============
* Added a dependence to datagate-rdbms library and used the wrapper class for Prepared statement in RDBMSBatchLoader

0.1.41
==============
* Add metrics instrumentation on RDBMS query & Batch uploader calls
* Upgrade pod memory to 3Gi

0.1.40
==============
* Add new GraphQL method Type_aggregate. This is reflected in getSDL endpoint.

0.1.39
==============
* Modify code and config to expose metrics via path "/metrics" 

0.1.38
==============
Add prometheus scehme to be https

0.1.37
==============
Update Prometheus config parameters

0.1.36
==============
Enable DataQueryLoader caching option

0.1.35
==============
Sanitize Sort and Filter objects for SQL injection

0.1.34
==============
* Add prometheus dependencies
* Add "/metrics" endpoint for prometheus scrapping 
* Add prometheus support in the Application code
* Modify K8 config to update the new endpoint
* Instrument metrics on GraphQLServlet
* Add local configuration file to run locally

0.1.33
==============
* updated Logging config

0.1.32
==============
* Sending logs to console to be captured by filebeat

0.1.31
==============
* Add prometheus annotations to scrape the metrics

0.1.30
==============
* Added swagger to servlet endpoints

0.1.29
==============
* Replace DataLoader EnumMap with a DataLoaderFactory

0.1.28
==============
* Regression test framework

0.1.27
==============
* Add k8s

0.1.26
==============
* Validate that field names in user arguments actually exist in the GraphQL schema

0.1.26
==============
* Support _FilterSet and non-null variables

0.1.25
==============
* Fix NullPointer issue when model type does not have attributes.

0.1.24
==============
* Upgrade dataquery to use V2 datamodel

0.1.23
==============
* Adding Hbase query operator

0.1.22
==============
* Support scalar and _Sort type variables
* Fix returning null values

0.1.21
==============
* Validate view type in the request path inside ServletFilter.
* Append viewType to the model name to generate resource which is passed to authorize method.

0.1.20
==============
* Named query framework
* 'Row lock get' and 'release' named queries
* Pass GQL variables to backend
* Use testNg in testing automation
* Use testNg in all unit tests
* Show data loader exceptions to user

0.1.19
==============
* Adding endpoint for getting SDL

0.1.18
==============
* Adding HBase Query

0.1.17
==============
* Updated the dm-datagate version to 0.0.8

0.1.16
==============
* Fixed the order of View and Model on method calls to DataGateSecurity

0.1.15
==============
* Removed the alloy sso dependence, now it uses datagate for authentication and authorization

0.1.14
==============
* Added custom GraphQL types
* Added unit tests for custom types
* Added new input type _Sort
* Added new new enum _SortDirection
* Modified query methods by adding _Sort type
* Fixed the uppercase issue in SQL
* Added sorting in SQL parser

0.1.13
==============
* Support non-null attributes in projections
* Support skip and limit arguments in RDBMS loader

0.1.12
==============
* RDBMS loader fixes

0.1.11
==============
* Update authz for Materialized view

0.1.10
==============
* Added datamodel healthcheck
* Move the SSOClient creation to DataqueryModel
* Applied Inject annotation

0.1.9
==============
* Recursive data fetcher fixes
* SQL parser tests
* Refactoring
* Minor fixes and improvements

0.1.8
==============
* added graphql endpoint, model service and guice module.

0.1.7
==============
* Recursive data fetcher
* Documentation

0.1.6
==============
* Data fetcher improvements
* Documentation

0.1.5
==============
* Adding Servlet Filter

0.1.4
==============
* Added RDBMS Data Loader Draft
* Documentation

0.1.3
==============
* Added Generic GraphQL Data Fetcher Draft

0.1.2
==============
* Added HBase api

0.1.1
==============
* Added missing license headers for the build to succeed

0.1.0
==============
* First Gradle Version
