/*
 * Copyright 2020 HM Revenue & Customs
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
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
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
}
