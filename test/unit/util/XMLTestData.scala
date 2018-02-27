/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.util

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.prop.TableDrivenPropertyChecks.Table

import scala.xml.Elem

object XMLTestData {

  val declarantEoriValue = "ZZ123456789000"
  val conversationIdValue = UUID.randomUUID().toString
  val correlationId = UUID.randomUUID().toString
  val clientId = UUID.randomUUID().toString
  val dateTime = DateTime.now(DateTimeZone.UTC)
  val dateTimeFormat = "YYYY-MM-dd'T'HH:mm:ss'Z'"


  val InvalidXML: Elem =
    <InventoryLinkingImportsValidateMovementResponse foo="bar" xmlns="http://gov.uk/customs/inventoryLinkingImport/v1"> <!--invalid-->
      <messageCode>str</messageCode>
      <dateOfEntry>2008-09-29T02:49:45</dateOfEntry>
      <entryNumber>string</entryNumber>
      <entryVersionNumber>3</entryVersionNumber>
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
      <irc>str</irc>
    </InventoryLinkingImportsValidateMovementResponse>

  val InvalidXMLWithMultipleErrors: Elem =
    <InventoryLinkingImportsValidateMovementResponse foo="bar" xmlns="http://gov.uk/customs/inventoryLinkingImport/v1"> <!--invalid-->
      <messageCode>str</messageCode>
      <dateOfEntry>2008-09-29T02:49:45</dateOfEntry>
      <entryNumber>abc_123</entryNumber> <!--invalid-->
      <entryVersionNumber>A</entryVersionNumber> <!--invalid-->
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
      <irc>str</irc>
    </InventoryLinkingImportsValidateMovementResponse>

  val ValidInventoryLinkingMovementRequestXML: Elem =
    <InventoryLinkingImportsValidateMovementResponse xmlns="http://gov.uk/customs/inventoryLinkingImport/v1">
      <messageCode>str</messageCode>
      <dateOfEntry>2008-09-29T02:49:45</dateOfEntry>
      <entryNumber>string</entryNumber>
      <entryVersionNumber>3</entryVersionNumber>
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
      <irc>str</irc>
    </InventoryLinkingImportsValidateMovementResponse>

  val ValidInventoryGoodsArrivalRequestXML: Elem =
    <inventoryLinkingImportsGoodsArrival xmlns:v1="http://gov.uk/customs/inventoryLinkingImport/v1">
      <messageCode>str</messageCode>
      <dateOfEntry>2008-09-29T02:49:45</dateOfEntry>
      <entryNumber>string</entryNumber>
      <!--Optional:-->
      <entryVersionNumber>3</entryVersionNumber>
      <goodsArrivalDeclaration>Y</goodsArrivalDeclaration>
      <inventoryConsignmentReference>string</inventoryConsignmentReference>
      <!--Optional:-->
      <irc>str</irc>
      <!--Optional:-->
      <transportNationality>st</transportNationality>
    </inventoryLinkingImportsGoodsArrival>

}
