/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import scala.xml.Elem

object XMLTestData {

  val InvalidInventoryLinkingMovementRequestXML: Elem =
    <InventoryLinkingImportsValidateMovementResponse foo="bar" xmlns="http://gov.uk/customs/inventoryLinkingImport/v1"> <!--invalid-->
      <entryNumber>string</entryNumber>
      <entryVersionNumber>3</entryVersionNumber>
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
      <irc>str</irc>
    </InventoryLinkingImportsValidateMovementResponse>

  val InvalidInventoryLinkingMovementRequestXMLWithMultipleErrors: Elem =
    <InventoryLinkingImportsValidateMovementResponse foo="bar" xmlns="http://gov.uk/customs/inventoryLinkingImport/v1"> <!--invalid-->
      <entryNumber>abc_123</entryNumber> <!--invalid-->
      <entryVersionNumber>A</entryVersionNumber> <!--invalid-->
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
      <irc>str</irc>
    </InventoryLinkingImportsValidateMovementResponse>

  val ValidInventoryLinkingMovementRequestXML: Elem =
    <InventoryLinkingImportsValidateMovementResponse xmlns="http://gov.uk/customs/inventoryLinkingImport/v1">
      <entryNumber>string</entryNumber>
      <entryVersionNumber>3</entryVersionNumber>
      <inventoryConsignmentReference>string with space</inventoryConsignmentReference>
      <irc>str</irc>
    </InventoryLinkingImportsValidateMovementResponse>

  val ValidInventoryLinkingGoodsArrivalRequestXML: Elem =
    <inventoryLinkingImportsGoodsArrival xmlns="http://gov.uk/customs/inventoryLinkingImport/v1">
      <entryNumber>string</entryNumber>
      <!--Optional:-->
      <entryVersionNumber>3</entryVersionNumber>
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
      <!--Optional:-->
      <transportNationality>st</transportNationality>
    </inventoryLinkingImportsGoodsArrival>

  val InvalidInventoryLinkingGoodsArrivalRequestXML: Elem =
    <inventoryLinkingImportsGoodsArrival foo="bar" xmlns="http://gov.uk/customs/inventoryLinkingImport/v1"> <!--invalid-->
      <entryNumber>string</entryNumber>
      <!--Optional:-->
      <entryVersionNumber>3</entryVersionNumber>
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
      <transportNationality>st</transportNationality>
    </inventoryLinkingImportsGoodsArrival>

  val InvalidInventoryLinkingGoodsArrivalRequestXMLWithMultipleErrors: Elem =
    <inventoryLinkingImportsGoodsArrival foo="bar" xmlns="http://gov.uk/customs/inventoryLinkingImport/v1"> <!--invalid-->
      <entryNumber>abc_123</entryNumber> <!--invalid-->
      <!--Optional:-->
      <entryVersionNumber>A</entryVersionNumber> <!--invalid-->
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
      <!--Optional:-->
      <transportNationality>st</transportNationality>
    </inventoryLinkingImportsGoodsArrival>

  val validWrappedGoodsArrivalXml: Elem = <n1:InventoryLinkingImportsInboundGoodsArrival xsi:schemaLocation="http://gov.uk/customs/inventoryLinkingImport/v1 request_schema.xsd" xmlns:n1="http://gov.uk/customs/inventoryLinkingImport/v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <n1:requestCommon>
      <n1:clientID>327d9145-4965-4d28-a2c5-39dedee50334</n1:clientID>
      <n1:conversationID>a71c2d56-2372-4c62-b29c-c78fd96b2c57</n1:conversationID>
      <n1:correlationID>3972b563-0a6f-4255-83c9-357a17f66a63</n1:correlationID>
      <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
      <n1:dateTimeStamp>2020-08-27T11:37:45Z</n1:dateTimeStamp>
      <n1:originatingPartyID>xsubmitterid123</n1:originatingPartyID>
      <n1:authenticatedPartyID>RASHADMUGHAL</n1:authenticatedPartyID>
    </n1:requestCommon>
    <n1:requestDetail>
      {ValidInventoryLinkingGoodsArrivalRequestXML}
    </n1:requestDetail>
  </n1:InventoryLinkingImportsInboundGoodsArrival>

  val validWrappedValidateMovementXml: Elem = <n1:InventoryLinkingImportsInboundValidateMovementResponse xsi:schemaLocation="http://gov.uk/customs/inventoryLinkingImport/v1 request_schema.xsd" xmlns:n1="http://gov.uk/customs/inventoryLinkingImport/v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <n1:requestCommon>
      <n1:clientID>327d9145-4965-4d28-a2c5-39dedee50334</n1:clientID>
      <n1:conversationID>375304fc-5c25-47e4-b160-b66dff49c2e0</n1:conversationID>
      <n1:correlationID>abCabC123u</n1:correlationID>
      <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
      <n1:dateTimeStamp>2020-08-27T11:44:48Z</n1:dateTimeStamp>
      <n1:originatingPartyID>xsubmitterid123</n1:originatingPartyID>
      <n1:authenticatedPartyID>RASHADMUGHAL</n1:authenticatedPartyID>
    </n1:requestCommon>
    <n1:requestDetail>
      {ValidInventoryLinkingMovementRequestXML}
    </n1:requestDetail>
  </n1:InventoryLinkingImportsInboundValidateMovementResponse>
  
}
