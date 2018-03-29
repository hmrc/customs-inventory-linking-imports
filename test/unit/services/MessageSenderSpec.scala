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

package unit.services

import java.net.URLEncoder
import java.util.UUID

import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.ACCEPTED
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ApiSubscriptionFieldsConnector, ImportsConnector, OutgoingRequest, OutgoingRequestBuilder}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.services._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData._
import util.TestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageSenderSpec extends UnitSpec with Matchers with MockitoSugar with TableDrivenPropertyChecks {

  trait SetUp {
    protected val importsMessageType: ImportsMessageType

    val httpResponse: AnyRef with HttpResponse = HttpResponse(ACCEPTED)
    val outgoingRequestBuilder = mock[OutgoingRequestBuilder]
    lazy val goodsArrivalXmlValidationService: GoodsArrivalXmlValidationService = mock[GoodsArrivalXmlValidationService]
    lazy val validateMovementXmlValidationService: ValidateMovementXmlValidationService = mock[ValidateMovementXmlValidationService]
    val importsConnector: ImportsConnector = mock[ImportsConnector]
    val apiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]

    lazy val requestData = mock[RequestData]

    implicit val validatedRequest: ValidatedRequest[AnyContent] = ValidatedRequest[AnyContent](requestData, request)

    val headers: HeaderMap = Map(HeaderConstants.XClientId -> TestXClientId, HeaderConstants.XBadgeIdentifier -> XBadgeIdentifierHeaderValueAsString)
    val sender: MessageSender = new MessageSender(apiSubscriptionFieldsConnector, outgoingRequestBuilder, goodsArrivalXmlValidationService, validateMovementXmlValidationService, importsConnector, mock[ImportsLogger])
    lazy val outgoingRequest = OutgoingRequest(serviceConfig, outgoingBody, validatedRequest)

    implicit val mockHeaderCarrier: HeaderCarrier = mock[HeaderCarrier]
    val request: Request[AnyContent] = mock[Request[AnyContent]]

    protected def service: XmlValidationService = importsMessageType match {
      case GoodsArrival => goodsArrivalXmlValidationService
      case ValidateMovement => validateMovementXmlValidationService
    }

    when(apiSubscriptionFieldsConnector.getClientSubscriptionId()(any[ValidatedRequest[AnyContent]])).thenReturn(Future.successful(FieldsId))
    when(validatedRequest.requestData.body).thenReturn(outgoingBody)
  }

  val messageTypes = Table(
    "Message type",
    GoodsArrival,
    ValidateMovement
  )

  forAll(messageTypes){(messageType) =>
    s"$messageType send" when {
      "message is valid" should {
        "return the result from the connector" in new SetUp {

          val importsMessageType: ImportsMessageType = messageType
          when(outgoingRequestBuilder.build(messageType, validatedRequest, FieldsId)).thenReturn(outgoingRequest)
          when(service.validate(outgoingBody)).thenReturn(Future.successful(()))
          when(importsConnector.post(outgoingRequest)).thenReturn(Future.successful(httpResponse))

          val actualResponse = await(sender.validateAndSend(messageType))

          actualResponse shouldBe httpResponse
        }
      }

      "message is invalid" should {
        "return failed future when validation service throws an exception" in new SetUp {

          val importsMessageType: ImportsMessageType = messageType
          when(service.validate(outgoingBody)).thenReturn(Future.failed(emulatedServiceFailure))

          val caught = intercept[EmulatedServiceFailure](await(sender.validateAndSend(messageType)))

          caught shouldBe emulatedServiceFailure
        }
      }
    }
  }
}
