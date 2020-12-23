# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]
## [1.2.5] - 2020-09-22
### Updated
- Added new test cases for coverage increase for row-level-security changes and reduced code smells

## [1.2.4] - 2020-09-02
### Changed
- Updated to latest app-state and conflux versions
### Added
- Row-level security feature changes

## [1.2.3] - 2020-08-27
### Updated
- Changed default container user from root to Alloy.

## [1.2.2] - 2020-08-11
### Added
- Kafka changes on DataQuery .

## [1.2.1] - 2020-07-28
### CHANGED
- named query sync response changes

## [1.1.26] - 2020-07-06
### CHANGED
- nexus url updates

## [1.1.25] - 2020-05-27
### Changed
- ALDM 552 Resolve Injection issue of GraphQLService .

## [1.1.24] - 2020-05-13
### Changed
- ALDM 552 Update the SDL cache by callback.

## [1.1.23] - 2020-04-22
### Changed
- Modified Health check code to prevent memory leakage.

## [1.1.22] - 2020-04-22
### Changed
- Disabled deep health check

## [1.1.21] - 2020-04-06
### Added
- Added NEWRELIC_AGENT_ENABLE flag from ConfigMpas

## [1.1.20] - 2020-04-03
### Changed
- Corrected the staging repository link in TestRecipe.yaml

## [1.1.19] - 2020-04-02
### Added
-Added the staging symmetric-key-path to file-mounts in k8s file

## [1.1.18] - 2020-03-25
### Updated
-Reverting the INFO loggers added for testing

## [1.1.17] - 2020-03-24
### Updated
-Added INFO loggers to verify Mutation behaviour

## [1.1.16] - 2020-03-23
### Added
- updated alloy-sso-client and datagate dependency version
- Epic link https://jira.opentext.com/browse/ALDM-126

## [1.1.15] - 2020-03-23
### Added
- Object security Implementation, Multi Query fix
- Epic link https://jira.opentext.com/browse/ALDM-126

## [1.1.14] - 2020-03-20
### Added
- Object security Implementation
- Epic link https://jira.opentext.com/browse/ALDM-126


## [1.1.13] - 2020-03-19
### Fixed
- ALDM-469 Providing wrong Query name in Json is not throwing any error

## [1.1.12] - 2020-03-18
### Added
- Minor changes on DataCast Json format on Mutation

## [1.1.11] - 2020-03-17
### Added
- Named-Query configurations added to conf/dev-dm-dataquery.yml

## [1.1.10] - 2020-03-16
### Added
- Implementation on Mutations

## [1.1.9] - 2020-03-13
### Added
- Support for postmap data format in named-query results
- Validation added for Model name

## [1.1.8] - 2020-03-04
### Added
- Support for implementing named sql queries instead of implementing java interfaces

## [1.1.7] - 2020-02-11
### Update
- Bumped Docker image version from 3.4.0 to 3.4.3

## [1.1.6] - 2020-01-07
### Added
- ALDM-241 Added New relic APM agent 

## [1.1.5] - 2020-01-07
### Added
- Updated the Slack Approval Waiting time (waitForApproval) to 31 DAYS in Jenkinsfile

## [1.1.4] - 2019-12-30
### Added
- Added Conflux 3.0.0 change with dependent module.

## [1.1.3] - 2019-10-18
### Updatedd
- ALDM-40 Added TestRecipe.yaml and modified Jenkinsfile.

## [1.1.2] - 2019-10-10
### Updatedd
- DM#2168 DataModel version updated to 1.0.181

## [1.1.1] - 2019-10-02
### Fixed
- DM#2162 Replace SonarQube credentials in all DM service Jenkins files

## [1.1.0] - 2019-08-22
### Added
- DM#1949 GraphQL Query Traverser V2: Left Joins + filters on nested structures

## [1.0.67] - 2019-08-19
### Fixed
- DM#2111 Increase liveliness and readiness probe timeouts 

## [1.0.66] - 2019-08-14
### Fixed
- DM#2104 DataQuery SDL default FK attributes missing _id

## [1.0.65] - 2019-07-31
### Changed
- DM#206 Update project dependencies

## [1.0.64] - 2019-07-26
### Changed
- DM 206 Change nexus repository Id, maven and build to update the dependencies 

## [1.0.63] - 2019-06-21
### Changed
- Made changes to deploy DataQuery to AT4 clusters only.

## [1.0.62] - 2019-06-20
### Changed
- Made MAX_JOIN_FIELD_COMPLEXITY, MAX_GRAPHQL_QUERY_COMPLEXITY fields configurable to use from configmaps.
- Made changes to deploy DataQuery to AT4 and PX1 clusters.

## [1.0.61] - 2019-06-12
### Changed
- Fixed datamodel health check when doing deep health check of DataQuery

## [1.0.60] - 2019-05-17
### Changed
- Make the NOT IN operation work with the filters and add regression test for that

## [1.0.59] - 2019-05-14
### Changed
- Run service level tests in QA environment as well

## [1.0.58] - 2019-05-14
### Changed
- Run service level tests in staging environment to take DQ to PROD

## [1.0.57] - 2019-05-01
### Added
- Rollback the previous changes for Jenkinsfile

## [1.0.56] - 2019-05-01
### Added
- Temorarily disable tests in Jenkinsfile

## [1.0.55] - 2019-05-01
### Added
- Regression tests added for the fix - Expose PK/FK fields as attributes in SDL

## [1.0.54] - 2019-05-01
### Fixed
- Use mock SSO client in RDBMSControl when SSO is disabled

## [1.0.53] - 2019-05-01
### Changed
- Use mock SSO client in authentication endpoint when SSO is disabled

## [1.0.52] - 2019-04-29
### Changed
- Expose PK/FK fields as attributes in SDL

## [1.0.51] - 2019-04-23
### Changed
- Update liveness and readiness probes to use simple health

## [1.0.50] - 2019-04-17
### Changed
- Fix the error message when the VIEW is missing from DataQuery request URL

## [1.0.49] - 2019-04-17
### Changed
- DataQuery throws an exception when foreign keys are null and GraphQL query tries to fetch the relationship data. 
  This issue is resolved by ignoring the relationship and null returned for these nested records. 

## [1.0.48] - 2019-04-15
### Changed
- Upgraded datamodel client version to 1.0.126 and changed datamodel configs in DQ to use base-url. 

## [1.0.47] - 2019-03-27
### Changed
- Set the proper status codes in case of exceptions. 

## [1.0.46] - 2019-03-27
### Changed
- Enable e2e test on Staging environment 

## [1.0.45] - 2019-03-06
### Changed
- Generation of bridgetablename logic in many-many relationships is changed based on datamodel logic.
- Fixed regression tests for the above change

## [1.0.44] - 2019-03-04
### Changed
- Comment out e2e tests in staging until the issue is resolved by e2e team

## [1.0.43] - 2019-02-27
### Changed
- Update Jenkins pipeline to add e2e test on staging
- Clean up the verbiages in stage headers
- Add a new stage for AE apporval 

## [1.0.42] - 2019-02-26
### Changed
- Reduce total complexity for introspection queries to allow introspection into complex models

## [1.0.41] - 2019-02-21
### Changed
- Comment out staging e2e until it's fixed as there is problem running it

## [1.0.40] - 2019-02-21
### Changed
- Change Jenkins to run e2e tests

## [1.0.39] - 2019-02-21
### Changed
- Fixed possible NullPointer in getIDField method of Query class
- Added regression tests to test above

## [1.0.38] - 2019-02-11
### Removed
- Comment out E2E test related references temporarily 

## [1.0.37] - 2019-02-11
### Removed
- Comment out E2E test on QA temporarily 

## [1.0.36] - 2019-02-11
### Removed
- Remove E2E test on QA and staging until E2E tests are fixed

## [1.0.35] - 2019-02-08
### Added
- Regression test for double byte characters

## [1.0.34] - 2019-02-07
### Removed
- Remove "minikube delete" from minikube-run.sh startup file
- Remove Java arguments to set max and min memory limits from build.gradle file
### Updated
- Update Docker file to pull image with Java version to 8u202 (alloy version: jre:1.10.2)
- Update deployment file to add JAVA_OPTS to unlock experimental feature for cgroup
- Update Garbage Collector to use G1GC

## [1.0.33] - 2019-02-06

### Updated

- Update minikube-run script to add minikube start-up commands
- Update minikube config map to add missing key

## [1.0.32] - 2019-02-04
### Removed
- "-TEST" from the version parameter for e2e

## [1.0.31] - 2019-02-04
### Removed
- 'testEnvironment' parameter from testSession creation for e2e

### Added
- 'TEST' at the end of the version for e2e 

### Changed
- Renaming of the variables for each e2e environments
- Code clean up

## [1.0.30] - 2019-02-01
### Changed
- E2E test includeGroups: 'dm-platform-e2e' to lowercase

## [1.0.29] - 2019-01-31
### Added
- E2E test on DEV and STAGING environment

### Changed
- E2E test to DM-PLATFORM-E2E on QA

## [1.0.28] - 2019-01-29
### Changed
- Temporarily skip the e2e test failures to see if the whole deployment pipeline works successfully

## [1.0.27] - 2019-01-29
### Changed
- Add ServiceNow work notes for Staging, UAT and Production deployments
- Add error when E2E test fails so that pipeline is broken

## [1.0.26] - 2019-01-28
### Changed
- Change production namespace to Namespace.PRODUCTION from Namespace.PROD

## [1.0.25] - 2019-01-23
### Added
- Slack notification for UAT deployment, parsing of release notes and some code clean up

## [1.0.24] - 2019-01-22
### Added
- Database names are made configurable using databaseSuffix
  Database suffix is added in configMapKey RDBMS_DB_NAME_SUFFIX

## [1.0.23] - 2019-01-22
### Added
- Add production jenkins pipeline 
- Add logic to fail pipeline if E2E test fails
- Add Slack message for UAT approval

## [1.0.22] - 2019-01-16
### Added
- Add testEnvironment: 'at4-qa' parameter to e2e test

## [1.0.21] - 2019-01-16
### Fixed
- Update Reporter XML filename for unit-test

## [1.0.20] - 2019-01-16
### Fixed
- Version endpoint

## [1.0.19] - 2019-01-15
### Changed
- Migrate scripts to use CHANGELOG

## [1.0.18] - 2019-01-15
### Changed
- Update the http response codes properly for unauthorized and forbidden

## [1.0.17] - 2019-01-11
### Changed
- Update jenkins pipeline for e2e test
- Update e2e test name 

## [1.0.17] - 2019-01-11
### Added
- E2E test call against DEV environment into Jenkins pipeline

## [1.0.16] - 2019-01-08
### Changed
- Update jenkins pipeline to deploy to staging
- Update jenkins to change the approval process of UAT to AE

## [1.0.15] - 2019-01-07
### Changed
- Migrate to CHANGELOG
