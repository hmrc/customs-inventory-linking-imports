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

package uk.gov.hmrc.customs.inventorylinking.imports.xml

import java.util.UUID

import org.joda.time.format.ISODateTimeFormat.dateTimeNoMillis
import play.api.mvc.AnyContent
import uk.gov.hmrc.customs.inventorylinking.imports.model.ValidatedRequest

import scala.xml.NodeSeq

class PayloadDecorator {
  def wrap(rdWrapper: ValidatedRequest[AnyContent], clientSubscriptionId: UUID, wrapperRootElementLabel: String): NodeSeq =
    <n1:rootElementToBeRenamed
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:n1="http://gov.uk/customs/inventoryLinkingImport/v1"
    xsi:schemaLocation="http://gov.uk/customs/inventoryLinkingImport/v1request_schema.xsd">
      <n1:requestCommon>
        <n1:clientID>{ clientSubscriptionId.toString }</n1:clientID>
        <n1:conversationID>{ rdWrapper.rdWrapper.conversationId }</n1:conversationID>
        <n1:correlationID>{ rdWrapper.rdWrapper.correlationId }</n1:correlationID>
        <n1:badgeIdentifier>{ rdWrapper.rdWrapper.badgeIdentifier.get }</n1:badgeIdentifier>
        <n1:dateTimeStamp>{ dateTimeNoMillis.print(rdWrapper.rdWrapper.dateTime)}</n1:dateTimeStamp>
      </n1:requestCommon>
      <n1:requestDetail>
        { rdWrapper.rdWrapper.body }
      </n1:requestDetail>
    </n1:rootElementToBeRenamed>.copy(label = wrapperRootElementLabel)
}
