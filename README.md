# Customs Inventory Linking Imports

The Customs Inventory Linking Imports service offers an interface for submitting Custom Inventory Linking Import declarations.

The objective of the Customs Inventory Linking Imports API is as below:

1. Receive a post request containing the import inventory linking declaration
3. Pass the request on to a backend service
4. Respond to the declarant indicating the success of previous steps.

It is assumed that the underlying backend process is asynchronous, and that the only response to the declarant from this API is to indicate the success (or otherwise) of the validation and submission to the backend service for onward processing.


## Development Setup
- Run locally: `sbt run` which runs on port `9824` by default
- Run with test endpoints: `sbt 'run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes'`

##  Service Manager Profiles
The Customs Inventory Linking Imports service can be run locally from Service Manager, using the following profiles:


| Profile Details                       | Command                                                           | Description                                                    |
|---------------------------------------|:------------------------------------------------------------------|----------------------------------------------------------------|
| CUSTOMS_DECLARATION_ALL               | sm2 --start CUSTOMS_DECLARATION_ALL                               | To run all CDS applications.                                   |
| CUSTOMS_INVENTORY_LINKING_EXPORTS_ALL | sm2 --start CUSTOMS_INVENTORY_LINKING_EXPORTS_ALL                 | To run all CDS Inventory Linking Exports related applications. |
| CUSTOMS_INVENTORY_LINKING_IMPORTS_ALL | sm2 --start CUSTOMS_INVENTORY_LINKING_IMPORTS_ALL                 | To run all CDS Inventory Linking Imports related applications. |


## Run Tests
- Run Unit Tests: `sbt test`
- Run Integration Tests: `sbt it:test`
- Run Unit and Integration Tests: `sbt test it:test`
- Run Unit and Integration Tests with coverage report: `./run_all_tests.sh`<br/> which runs `sbt clean coverage test it:test coverageReport"`

### Acceptance Tests
To run the CDS acceptance tests, see [here](https://github.com/hmrc/customs-automation-test).

### Performance Tests
To run performance tests, see [here](https://github.com/hmrc/customs-declaration-performance-test).


## API documentation
For Customs Inventory Linking Imports documentation, see [here](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/customs-inventory-linking-imports).


### Customs Inventory Linking Imports specific routes
| Path - internal routes prefixed by `/customs/inventory-linking-imports` | Supported Methods | Description                                                                                        |
|-------------------------------------------------------------------------|:-----------------:|----------------------------------------------------------------------------------------------------|
| `/arrival-notifications`                                                |       POST        | Submission of a CSP's request to present their goods to Customs.                                   |
| `/movement-validation`                                                  |       POST        | Submission of a CSP's validate movement response in order to match to a Inventory Exports Request. |


### Test-only specific routes
This service does not have any specific test-only endpoints.


## Seeding Data in `api-subscription-fields` for local end to end testing

Make sure the `api-subscription-fields` service is running on port `9650`. Then run the below curl command.
 - Note that the UUID `6372609a-f550-11e7-8c3f-9a214cf093aa` is the application clientId.

```
curl -v -X PUT "http://localhost:9650/field/application/6372609a-f550-11e7-8c3f-9a214cf093aa/context/customs%2Finventory-linking-imports/version/1.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "https://postman-echo.com/post", "securityToken" : "securityToken", "authenticatedEori": "SOMEAUTHEORI" } }'
```

### Seeding data for the `customs-notifications-receiver-stub` dummy callback endpoint

This endpoint can be used for the `callbackUrl` field above. For more information on how it can be used and seeded please 
see the [README](https://github.com/hmrc/customs-notifications-receiver-stub/blob/master/README.md)   

# Switching service endpoints

Dynamic switching of service endpoints has been implemented for inventory linking imports connector. To configure dynamic
switching of the endpoint there must be a corresponding section in the application config file
(see example below). This should contain the endpoint config details.

## Example
The service `customs-inventory-linking-imports` has a `default` configuration and a `stub` configuration. Note
that `default` configuration is declared directly inside the `customs-inventory-linking-imports` section.
```
  services {
    ...

    validatemovement {
      host = some.host
      port = 80
      bearer-token = "some_token"
      context = "/imports-sample-context"

      stub {
        host = localhost
        port = 9479
        bearer-token = "some_stub_token"
         context = "/imports-sample-context"
      }
    }
  }
```
## Useful CURL commands for local testing
[link to curl commands](docs/curl-commands.md)
    
### Switch service configuration for an endpoint

#### REQUEST
    default:
    curl -X "POST" http://customs-inventory-linking-imports-host/test-only/service/validatemovement/configuration -H 'content-type: application/json' -d '{ "environment": "stub" }'

#### RESPONSE

    The service customs-inventory-linking-imports is now configured to use the stub environment

### Switch service configuration to default for an endpoint

#### REQUEST

    curl -X "POST" http://customs-inventory-linking-imports-host/test-only/service/validatemovement/configuration -H 'content-type: application/json' -d '{ "environment": "default" }'

#### RESPONSE

    The service customs-inventory-linking-imports is now configured to use the default environment

### Get the current configuration for a service

#### REQUEST

    curl -X "GET" http://customs-inventory-linking-imports-host/test-only/service/validatemovement/configuration

#### RESPONSE

    {
      "service": "validatemovement",
      "environment": "stub",
      "url": "http://currenturl/customs-inventory-linking-imports"
      "bearerToken": "current token"
    }

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")