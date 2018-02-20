# customs-inventory-linking-imports

[![Build Status](https://travis-ci.org/hmrc/customs-inventory-linking-imports.svg)](https://travis-ci.org/hmrc/customs-inventory-linking-imports) [ ![Download](https://api.bintray.com/packages/hmrc/releases/customs-inventory-linking-imports/images/download.svg) ](https://bintray.com/hmrc/releases/customs-inventory-linking-imports/_latestVersion)

This service offers an interface for submitting custom inventory linking import declarations.

The objective of the Customs Inventory Linking Imports API is as below:
 
1. Receive a post request containing the import inventory linking declaration
3. Pass the request on to a backend service
4. Respond to the declarant indicating the success of previous steps.

It is assumed that the underlying backend process is asynchronous, and that the only response to the declarant from this API is to indicate the success (or otherwise) of the validation and submission to the backend service for onward processing.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")