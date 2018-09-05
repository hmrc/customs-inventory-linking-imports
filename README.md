# customs-inventory-linking-imports

[![Build Status](https://travis-ci.org/hmrc/customs-inventory-linking-imports.svg)](https://travis-ci.org/hmrc/customs-inventory-linking-imports) [ ![Download](https://api.bintray.com/packages/hmrc/releases/customs-inventory-linking-imports/images/download.svg) ](https://bintray.com/hmrc/releases/customs-inventory-linking-imports/_latestVersion)

This service offers an interface for submitting custom inventory linking import declarations.

The objective of the Customs Inventory Linking Imports API is as below:

1. Receive a post request containing the import inventory linking declaration
3. Pass the request on to a backend service
4. Respond to the declarant indicating the success of previous steps.

It is assumed that the underlying backend process is asynchronous, and that the only response to the declarant from this API is to indicate the success (or otherwise) of the validation and submission to the backend service for onward processing.

## Seeding Data in `api-subscription-fields` for local end to end testing

Make sure the `api-subscription-fields` service is running on port `9650`. Then run the below curl command.
 - Note that the UUID `d8f027b1-e280-4794-b4fa-4d6a47ca0a67` is the application clientId.
 - api context value `customs%2Finventory-linking-imports` is hardcoded in `MessageSender`, please make sure you use it in PUT request
 
```
curl -v -X PUT "http://localhost:9650/field/application/d8f027b1-e280-4794-b4fa-4d6a47ca0a67/context/customs%2Finventory-linking-imports/version/1.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "https://postman-echo.com/post", "securityToken" : "securityToken" } }''
```

## Sample request for local testing

```
curl -v -X POST -H 'Accept: application/vnd.hmrc.1.0+xml' -H 'Content-Type: application/xml; charset=UTF-8' -H 'Authorization: Bearer CSP' -H 'X-Badge-Identifier: BADGEID123' -H 'X-Client-ID: d8f027b1-e280-4794-b4fa-4d6a47ca0a67' -d ' <inventoryLinkingImportsGoodsArrival xmlns="http://gov.uk/customs/inventoryLinkingImport/v1">
 <entryNumber>string</entryNumber>
 <entryVersionNumber>3</entryVersionNumber>
 <inventoryConsignmentReference>string</inventoryConsignmentReference>
 <transportNationality>st</transportNationality>
 </inventoryLinkingImportsGoodsArrival>' 'http://localhost:9824/arrival-notifications'
```

# Switching service endpoints

Dynamic switching of service endpoints has been implemented for inventory linking imports connector. To configure dynamic
switching of the endpoint there must be a corresponding section in the application config file
(see example below). This should contain the endpoint config details.

## Example
The service `customs-inventory-linking-imports` has a `default` configuration and a `stub` configuration. Note
that `default` configuration is declared directly inside the `customs-inventory-linking-imports` section.

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