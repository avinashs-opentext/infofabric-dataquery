# DM Data Query Service
DM Data Query Service provides a generic GraphQL query API that works across all Data Management Materialized Views. 

Start with the [User Guide](https://confluence.liaison.tech/display/DM/DM+Data+Query+User+Guide)

Data Query Service uses [Data Gate Libraries](https://github.com/LiaisonTechnologies/dm-datagate) to connect to Materialized Views. 

[Query Service on Read Framework NPD](https://confluence.liaison.tech/display/ALLOY/Materialized+Views+Read+Framework#MaterializedViewsReadFramework-QueryService)

[Query Service on Transactional Data Layer NPD](https://confluence.liaison.tech/display/ALLOY/DM+Transactional+Data+Layer#DMTransactionalDataLayer-GenericWriteFramework)
## Data Query endpoints

### GraphQL queries

`POST /1/views/query/graphql/{tenant}/{model}/{view}`

```
{
  "query": <GraphQL query>
}
```

### Named Queries

`POST /1/views/query/named/{tenant}/{model}/{view}/{info}`

```
{
  "operationName": <Query Name>,
  "variables": <Map of query variables>
}
```

{info} can be:
* list - list all available query names for the view type
* info - show information about a named query (operationName required)
* preview - render named query (operationName and variables required)
* [empty] - execute named query

## Data Mutation endpoints

`POST /1/views/mutation/graphql/{tenant}/{connector-name}`

## User Control CRUD endpoints

CRUD operations for UserControls used by [Data Gate Library's](https://github.com/LiaisonTechnologies/dm-datagate) Functional Control Library (fcl) project.

|Method  |Endpoint                                              |                                                                                            | 
|--------|------------------------------------------------------|--------------------------------------------------------------------------------------------| 
| DELETE | /1/views/controls/{id}                               | Deletes an existing control for the id                                                     | 
| GET    | /1/views/controls/all/{view}                         | Returns all "view" specific controls (where the other attributes were configured w/ "*")"  | 
| GET    | /1/views/controls/all/{view}/{tenant}                | Returns all "tenant" specific controls (where the other attributes were configured w/ "*") | 
| GET    | /1/views/controls/all/{view}/{tenant}/{model}        | Returns all "model" specific controls (where the other attributes were configured w/ "*")  | 
| GET    | /1/views/controls/all/{view}/{tenant}/{model}/{user} | Returns all "user" specific controls                                                       | 
| GET    | /1/views/controls/{id}                               | Gets a control by id                                                                       | 
| PATCH  | /1/views/controls/{id}                               | Updates an existing control for the id with the provided UserControls body                 | 
| POST   | /1/views/controls                                    | Creates a new control with the provided UserControls body                                  | 
| PUT    | /1/views/controls/{id}                               | Replaces an existing control for the id with the provided UserControls body                | 


## Development

### Implementing data loaders

Each SDL has a `_GraphQLQuery` type that `RootDataFetcher` gets wired to, meaning that all query methods will be resolved using the `RootDataFetcher`. 
`RootDataFetcher` will traverse the query (using `DataQueryTraversal`) and build `Query` objects that will be supplied to `DataLoaders`.

`Query` object has following properties:
* Collection - Name of the collection/table etc.
* ProjectionSet - List of fields to return for the object from the collection/table etc.
* FilterSet - List of filters and nested filter sets (examples in the [User Guide](https://confluence.liaison.tech/display/DM/DM+Data+Query+User+Guide))
* Skip - Number of records to skip for pagination
* Limit - Maximum number of records to return

Every new DataLoader needs to implement `DataQueryBatchLoader` and its three methods: `validate` and `getNativeQuery` are convenience methods for clients, `load` method is where the actual data loading will be done. 
`Load` method will receive a list of `Query` objects, and each `Query` object needs to return a single `ResultSet` so that count of returned `ResultSets` is equal to the count of supplied `Query` objects. It's up to the data loader how these queries get batched for data base querying. 

`Query 1 ---- 1 ResultSet`

Every `Query` object within a single load call will target the same `Collection` (table) to make batching simpler.


``` java
package com.liaison.dataquery.graphql.dataloaders.rdbms;

public class MyBatchLoader implements DataQueryBatchLoader {

    @Override
    public CompletionStage<List<ResultSet>> load(List<Query> queries) {
        //Note: Load needs to return as many ResultSets as there are Query objects.
        return CompletableFuture.supplyAsync(() -> {
            try {
                String myQuery = MyQueryParser.parse(queries);
                // ... execute query, build a list of resultsets
                return resultsets
            } catch (Exception e) {
                logger.error("Error running query", e);
                throw new DataqueryRuntimeException(e);
            }          
        });
    }

    @Override
    public void validate(List<Query> queryKeys) {
        MyQueryParser.validate(queryKeys);
    }

    @Override
    public Object getNativeQuery(List<Query> queries) {
        return MyQueryParser.parse(queries);
    }
}
```


### Regression unit tests

Test resource folder contains a `regressiontests` folder that is used by RDBMSRegressionTests class. The folder is scanned by the unit test and test are executed if following files are found:

- datamodel.json: simple version of the datamodel to be tested. Must contain type names, and attribute names and data types
- (type).csv: data to be populated in the in memory database. Type must match datamodel type and columns attribute names
- q(query_name).graphql: GraphQL query to be tested
- q(query_name).json: expected result
- q(query_name)_vars.json: query variables (optional)