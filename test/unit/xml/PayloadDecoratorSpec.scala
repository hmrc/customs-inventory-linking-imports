/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.xml

import org.scalatest.mockito.MockitoSugar
import play.api.mvc.AnyContentAsXml
import uk.gov.hmrc.customs.inventorylinking.imports.model.ApiSubscriptionFieldsResponse
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, TestData}

import scala.xml.NodeSeq

class PayloadDecoratorSpec extends UnitSpec with MockitoSugar with ApiSubscriptionFieldsTestData {

  private val xml: NodeSeq = <node1></node1>

  private val payloadDecorator = new PayloadDecorator
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest
  private def wrapPayloadWithoutCorrelationId(apiSubscriptionFieldsResponse: ApiSubscriptionFieldsResponse = apiSubscriptionFieldsResponse) = payloadDecorator.wrap(xml, apiSubscriptionFieldsResponse, None, "InventoryLinkingImportsInboundValidateMovementResponse", RequestDateTime, CorrelationIdUuid)
  private def wrapPayload(apiSubscriptionFieldsResponse: ApiSubscriptionFieldsResponse = apiSubscriptionFieldsResponse) = payloadDecorator.wrap(xml, apiSubscriptionFieldsResponse, Some(ValidCorrelationIdHeader), "InventoryLinkingImportsInboundValidateMovementResponse", RequestDateTime, CorrelationIdUuid)

  "PayloadDecorator" should {

    "set the root element label" in {
      val result = wrapPayload()

      result \\ "InventoryLinkingImportsInboundValidateMovementResponse" should not be empty
    }

    "wrap passed XML in wrapper" in {
      val result = wrapPayload()

      val reqDet = result \\ "requestDetail"
      reqDet.head.child.contains(<node1 />) shouldBe true
    }

    "set the dateTimeStamp in the wrapper" in {
      val result = wrapPayload()

      val rd = result \\ "dateTimeStamp"

      rd.head.text shouldBe RequestDateTimeHttp
    }

    "set the conversationId" in {
      val result = wrapPayload()

      val rd = result \\ "conversationID"

      rd.head.text shouldBe ValidConversationId.toString
    }

    "set the clientId" in {
      val result = wrapPayload()

      val rd = result \\ "clientID"

      rd.head.text shouldBe fieldsId.value
    }

    "set the correlationID" in {
      val result = wrapPayload()

      val rd = result \\ "correlationID"

      rd.head.text shouldBe ValidCorrelationIdHeader.toString
    }

    "set the badgeIdentifier" in {
      val result = wrapPayload()

      val rd = result \\ "badgeIdentifier"

      rd.head.text shouldBe ValidBadgeIdentifierValue
    }

    "set the authenticatedpartyID" in {
      val result = wrapPayload()

      val rd = result \\ "authenticatedpartyID"

      rd.head.text shouldBe AuthenticatedEoriValue
    }

    "not set the authenticatedpartyID when not present" in {
      val result = wrapPayload(apiSubscriptionFieldsResponseWithoutAuthenticatedEori)

      val rd = result \\ "authenticatedpartyID"

      rd shouldBe NodeSeq.Empty
    }

    "use the UUID correlationID when not sent in the header" in {
      val result = wrapPayloadWithoutCorrelationId()

      val rd = result \\ "correlationID"

      rd.head.text shouldBe CorrelationIdValue
    }
  }
}
