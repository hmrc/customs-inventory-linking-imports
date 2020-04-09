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

package uk.gov.hmrc.customs.inventorylinking.imports.xml

import java.util.UUID

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ApiSubscriptionFieldsResponse, CorrelationIdHeader}

import scala.xml.{NodeSeq, Text}

class PayloadDecorator {

  private val newLineAndIndentation = "\n        "

  def wrap[A](xml: NodeSeq,
              apiSubscriptionFieldsResponse: ApiSubscriptionFieldsResponse,
              correlationIdHeader: Option[CorrelationIdHeader],
              wrapperRootElementLabel: String,
              dateTime: DateTime,
              correlationId: UUID)(implicit vpr: ValidatedPayloadRequest[A]): NodeSeq = {

    val badgeIdentifierElement = vpr.maybeBadgeIdentifier.fold(Seq(NodeSeq.Empty)) { badgeIdentifier => Seq[NodeSeq](<n1:badgeIdentifier>{badgeIdentifier.value}</n1:badgeIdentifier>,Text(newLineAndIndentation))}
    val dateTimeStampElement = Seq[NodeSeq](<n1:dateTimeStamp>{dateTime.toString(ISODateTimeFormat.dateTimeNoMillis)}</n1:dateTimeStamp>, Text(newLineAndIndentation))
    val originatingPartyElement = vpr.maybeSubmitterIdentifier.fold(Seq(NodeSeq.Empty)){ submitterIdentifier => Seq[NodeSeq](<n1:originatingPartyID>{submitterIdentifier.value}</n1:originatingPartyID>, Text(newLineAndIndentation) )}
    val authenticatedPartyElement = apiSubscriptionFieldsResponse.fields.authenticatedEori.fold(NodeSeq.Empty){ authenticatedEori: String => <n1:authenticatedPartyID>{authenticatedEori}</n1:authenticatedPartyID>}

    <n1:rootElementToBeRenamed
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:n1="http://gov.uk/customs/inventoryLinkingImport/v1"
    xsi:schemaLocation="http://gov.uk/customs/inventoryLinkingImport/v1 request_schema.xsd">
      <n1:requestCommon>
        <n1:clientID>{apiSubscriptionFieldsResponse.fieldsId.toString}</n1:clientID>
        <n1:conversationID>{vpr.conversationId.toString}</n1:conversationID>
        <n1:correlationID>{correlationIdHeader.fold(correlationId.toString) { corrId: CorrelationIdHeader => corrId.value }}</n1:correlationID>
        {badgeIdentifierElement}{dateTimeStampElement}{originatingPartyElement}{authenticatedPartyElement}
      </n1:requestCommon>
      <n1:requestDetail>
        {xml}
      </n1:requestDetail>
    </n1:rootElementToBeRenamed>.copy(label = wrapperRootElementLabel)
  }
}
