# Customs Inventory Linked Imports Curl Commmands
---
### Endpoints Summary

| Path                                                                                                                            |  Method  | Description                                |
|---------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------|
| [`/arrival-notifications`](#user-content-post-arrival-notifications)                                                              |   `POST` |    Allows submission of a arrival notification | 
| [`/movement-validation`](#user-content-post-movement-validation)                                                            |   `POST` |    Allows submission of movement validation request |

--- 
 
### POST Customs Declaration Inventory Linking Imports 
#### `POST /arrival-notifications`
Submits a new arrival notification
 
##### curl command
```
curl -v -X POST "http://localhost:9824/arrival-notifications" \
  -H 'Accept: application/vnd.hmrc.1.0+xml' \
  -H 'Authorization: Bearer {ADD VALID TOKEN}' \
  -H 'Content-Type: application/xml;charset=utf-8' \
  -H 'X-Badge-Identifier: {Badge Id}' \
  -H 'X-Client-ID: {Valid Client Id}' \
  -H 'cache-control: no-cache' \
  -H 'X-Submitter-Identifier: {Submitter Id}' \
 -d '<?xml version="1.0" encoding="UTF-8"?>
     <inv:inventoryLinkingImportsGoodsArrival xmlns:inv="http://gov.uk/customs/inventoryLinkingImport/v1">
        <inv:entryNumber>string</inv:entryNumber>
        <inv:entryVersionNumber>3</inv:entryVersionNumber>
        <inv:inventoryConsignmentReference>string</inv:inventoryConsignmentReference>
        <inv:transportNationality>st</inv:transportNationality>
     </inv:inventoryLinkingImportsGoodsArrival>'
```
 
---

### POST Movement Validation
#### `POST /movement-validation`
Submits a customs movement validation
 
##### curl command
```
curl -v -X POST "http://localhost:9824/movement-validation" \
  -H 'Accept: application/vnd.hmrc.1.0+xml' \
  -H 'Authorization: Bearer {ADD VALID TOKEN}' \
  -H 'Content-Type: application/xml;charset=utf-8' \
  -H 'X-Badge-Identifier: {Badge Id}' \
  -H 'X-Client-ID: {Valid Client Id}' \
  -H 'cache-control: no-cache' \
  -H 'X-Submitter-Identifier: {Submitter Id}' \
  -H 'X-Correlation-ID: {Correlation Id}' \
 -d '<?xml version="1.0" encoding="UTF-8"?>
     <inv:InventoryLinkingImportsValidateMovementResponse xmlns:inv="http://gov.uk/customs/inventoryLinkingImport/v1">
        <inv:entryNumber>string</inv:entryNumber>
        <inv:entryVersionNumber>3</inv:entryVersionNumber>
        <inv:inventoryConsignmentReference>string</inv:inventoryConsignmentReference>
        <inv:irc>str</inv:irc>
     </inv:InventoryLinkingImportsValidateMovementResponse>'
```

