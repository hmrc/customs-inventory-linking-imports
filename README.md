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
 - Note that the UUID `6372609a-f550-11e7-8c3f-9a214cf093aa` is the application clientId.

```
curl -v -X PUT "http://localhost:9650/field/application/6372609a-f550-11e7-8c3f-9a214cf093aa/context/customs%2Finventory-linking-imports/version/1.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "http://localhost:9826/customs-notifications-receiver-stub/pushnotifications", "securityToken" : "securityToken" } }'
```

This will generate a MongoDb record in the `notifications` collection in the `api-subscription-fields` database:

```json
{
    "_id" : ObjectId("5b8fab9b9bb4d54201571201"),
    "apiContext" : "customs/inventory-linking-imports",
    "apiVersion" : "1.0",
    "clientId" : "6372609a-f550-11e7-8c3f-9a214cf093aa",
    "fields" : {
        "callbackUrl" : "http://localhost:9826/customs-notifications-receiver-stub/pushnotifications",
        "securityToken" : "securityToken"
    },
    "fieldsId" : "f369eb7e-e6bf-42c6-9902-3c70705684e8"
}
```

Note if you want to use the `customs-notifications-receiver-stub` to receive notifications in a MongoDB database you will then have to update the `securityToken` 
field in the database with `fieldsId` value.
This is because the `customs-notifications-receiver-stub` reads the authorisation header in order to extract the client subscription fields id. So after 
doing this the above record will look like:

```json
{
    "_id" : ObjectId("5b8fab9b9bb4d54201571201"),
    "apiContext" : "customs/inventory-linking-imports",
    "apiVersion" : "1.0",
    "clientId" : "6372609a-f550-11e7-8c3f-9a214cf093aa",
    "fields" : {
        "callbackUrl" : "http://localhost:9826/customs-notifications-receiver-stub/pushnotifications",
        "securityToken" : "f369eb7e-e6bf-42c6-9902-3c70705684e8"
    },
    "fieldsId" : "f369eb7e-e6bf-42c6-9902-3c70705684e8"
}
```

Please see the readme for the `customs-notifications-receiver-stub` service for a description of CRUD endpoints that it offers.


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