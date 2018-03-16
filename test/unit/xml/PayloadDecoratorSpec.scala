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

package unit.xml

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestDataWrapper
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData._
import util.TestData._

import scala.xml.NodeSeq

class PayloadDecoratorSpec extends UnitSpec with MockitoSugar {

  private val xml: NodeSeq = <node1></node1>

  private val payloadWrapper = new PayloadDecorator
  private val rdWrapperMock = mock[RequestDataWrapper]

  private def wrapPayload() = payloadWrapper.wrap(rdWrapperMock, FieldsId, "InventoryLinkingImportsInboundValidateMovementResponse")

  "PayloadWrapper" should {

    when(rdWrapperMock.body).thenReturn(xml)
    when(rdWrapperMock.badgeIdentifier).thenReturn(Some(XBadgeIdentifierHeaderValueAsString))
    when(rdWrapperMock.conversationId).thenReturn(conversationId.toString)
    when(rdWrapperMock.correlationId).thenReturn(correlationId.toString)
    when(rdWrapperMock.dateTime).thenReturn(requestDateTime)

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

      rd.head.text shouldBe requestDateTimeHttp
    }

    "set the conversationId" in {
      val result = wrapPayload()

      val rd = result \\ "conversationID"

      rd.head.text shouldBe conversationId.toString
    }

    "set the clientId" in {
      val result = wrapPayload()

      val rd = result \\ "clientID"

      rd.head.text shouldBe FieldsIdAsString
    }

    "set the correlationID" in {
      val result = wrapPayload()

      val rd = result \\ "correlationID"

      rd.head.text shouldBe correlationId.toString
    }

    "set the badgeIdentifier" in {
      val result = wrapPayload()

      val rd = result \\ "badgeIdentifier"

      rd.head.text shouldBe XBadgeIdentifierHeaderValueAsString
    }
  }
}
