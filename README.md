# customs-inventory-linking-imports

[![Build Status](https://travis-ci.org/hmrc/customs-inventory-linking-imports.svg)](https://travis-ci.org/hmrc/customs-inventory-linking-imports) [ ![Download](https://api.bintray.com/packages/hmrc/releases/customs-inventory-linking-imports/images/download.svg) ](https://bintray.com/hmrc/releases/customs-inventory-linking-imports/_latestVersion)

This service offers an interface for submitting custom inventory linking import declarations.

The objective of the Customs Inventory Linking Imports API is as below:

1. Receive a post request containing the import inventory linking declaration
3. Pass the request on to a backend service
4. Respond to the declarant indicating the success of previous steps.

It is assumed that the underlying backend process is asynchronous, and that the only response to the declarant from this API is to indicate the success (or otherwise) of the validation and submission to the backend service for onward processing.

# Switching service endpoints

Dynamic switching of service endpoints has been implemented for wco declaration connector. To configure dynamic
switching of the endpoint there must be a corresponding section in the application config file
(see example below). This should contain the endpoint config details.


## Example
The service `customs-inventory-linking-imports` has a `default` configuration and a `stub` configuration. Note
that `default` configuration is declared directly inside the `customs-inventory-linking-imports` section.

    Prod {
        ...
      services {
        ...
    
        imports {
          host = some.host
          port = 80
          bearer-token = "some_token"
          context = "/imports"
    
          stub {
            host = localhost
            port = 9479
            bearer-token = "some_stub_token"
             context = "/imports"
          }
        }
      }
    }
    
### Switch service configuration for an endpoint

#### REQUEST
    default:
    curl -X "POST" http://customs-inventory-linking-imports-host/test-only/service/imports/configuration -H 'content-type: application/json' -d '{ "environment": "stub" }'

#### RESPONSE

    The service customs-inventory-linking-imports is now configured to use the stub environment

### Switch service configuration to default for an endpoint

#### REQUEST

    curl -X "POST" http://customs-inventory-linking-imports-host/test-only/service/imports/configuration -H 'content-type: application/json' -d '{ "environment": "default" }'

#### RESPONSE

    The service customs-inventory-linking-imports is now configured to use the default environment

### Get the current configuration for a service

#### REQUEST

    curl -X "GET" http://customs-inventory-linking-imports-host/test-only/service/imports/configuration

#### RESPONSE

    {
      "service": "imports",
      "environment": "stub",
      "url": "http://currenturl/customs-inventory-linking-imports"
      "bearerToken": "current token"
    }

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")