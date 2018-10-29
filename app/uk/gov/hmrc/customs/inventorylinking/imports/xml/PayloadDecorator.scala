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

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ApiSubscriptionFieldsResponse, CorrelationIdHeader}

import scala.xml.NodeSeq

class PayloadDecorator {
  def wrap[A](xml: NodeSeq,
              apiSubscriptionFieldsResponse: ApiSubscriptionFieldsResponse,
              correlationId: CorrelationIdHeader,
              wrapperRootElementLabel: String,
              dateTime: DateTime)(implicit vpr: ValidatedPayloadRequest[A]): NodeSeq =

    <n1:rootElementToBeRenamed
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:n1="http://gov.uk/customs/inventoryLinkingImport/v1"
    xsi:schemaLocation="http://gov.uk/customs/inventoryLinkingImport/v1request_schema.xsd">
      <n1:requestCommon>
        <n1:clientID>{ apiSubscriptionFieldsResponse.fieldsId.toString }</n1:clientID>
        <n1:conversationID>{ vpr.conversationId.toString}</n1:conversationID>
        <n1:correlationID>{ correlationId.toString }</n1:correlationID>
        <n1:badgeIdentifier>{ vpr.badgeIdentifier.value }</n1:badgeIdentifier>
        <n1:dateTimeStamp>{ dateTime.toString(ISODateTimeFormat.dateTimeNoMillis) }</n1:dateTimeStamp>
        <n1:submitterID>{ vpr.submitterIdentifier.value }</n1:submitterID>
        {
          apiSubscriptionFieldsResponse.fields.authenticatedEori.fold(NodeSeq.Empty) { authenticatedEori: String => <n1:authenticatedpartyID>{authenticatedEori}</n1:authenticatedpartyID>}
        }
      </n1:requestCommon>
      <n1:requestDetail>
        { xml }
      </n1:requestDetail>
    </n1:rootElementToBeRenamed>.copy(label = wrapperRootElementLabel)
}
