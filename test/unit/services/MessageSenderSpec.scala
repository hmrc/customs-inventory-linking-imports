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

import org.mockito.ArgumentMatchers.{eq => ameq, any}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.Status.ACCEPTED
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ApiSubscriptionFieldsConnector, ImportsConnector, OutgoingRequest, OutgoingRequestBuilder}
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
    val headers: Map[String, String] = Map(HeaderNames.XClientId -> TestXClientId, HeaderNames.XBadgeIdentifier -> XBadgeIdentifierHeaderValueAsString)
    val sender: MessageSender = new MessageSender(apiSubscriptionFieldsConnector, outgoingRequestBuilder, goodsArrivalXmlValidationService, validateMovementXmlValidationService, importsConnector)
    lazy val outgoingRequest = OutgoingRequest(serviceConfig, body, requestInfo)
    private val apiContextEncoded = URLEncoder.encode("customs/inventory-linking-imports", "UTF-8")

    implicit val mockHeaderCarrier: HeaderCarrier = mock[HeaderCarrier]
    val request: Request[AnyContent] = mock[Request[AnyContent]]
    implicit val data: RequestDataWrapper = mock[RequestDataWrapper]

    protected def service: XmlValidationService = importsMessageType match {
      case GoodsArrival => goodsArrivalXmlValidationService
      case ValidateMovement => validateMovementXmlValidationService
    }

    when(apiSubscriptionFieldsConnector.getSubscriptionFields(ameq(ApiSubscriptionKey(TestXClientId, apiContextEncoded, "1.0")))(any[RequestDataWrapper])).thenReturn(Future.successful(TestApiSubscriptionFieldsResponse))
    when(data.body).thenReturn(body)
    when(data.requestInfo).thenReturn(requestInfo)
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
          when(outgoingRequestBuilder.build(messageType, requestInfo, TestFieldsId, XBadgeIdentifierHeaderValue, body)).thenReturn(outgoingRequest)
          when(service.validate(body)).thenReturn(Future.successful(()))
          when(importsConnector.post(outgoingRequest)).thenReturn(Future.successful(httpResponse))
          when(data.headers).thenReturn(headers)
          val actualResponse = await(sender.validateAndSend(messageType))

          actualResponse shouldBe httpResponse
        }
      }

      "message is invalid" should {
        "return failed future when validation service throws an exception" in new SetUp {
          val importsMessageType: ImportsMessageType = messageType
          when(service.validate(body)).thenReturn(Future.failed(emulatedServiceFailure))
          when(data.headers).thenReturn(headers)
          val caught = intercept[EmulatedServiceFailure](await(sender.validateAndSend(messageType)))

          caught shouldBe emulatedServiceFailure
        }

        "return failed future when expected header X-Client-ID is not present" in new SetUp {
          val importsMessageType: ImportsMessageType = messageType
          when(service.validate(body)).thenReturn(Future.successful(()))
          val headersWithOnlyXBadgeIdentifier = Map(HeaderNames.XBadgeIdentifier -> XBadgeIdentifierHeaderValueAsString)
          when(data.headers).thenReturn(headersWithOnlyXBadgeIdentifier)
          val caught = intercept[IllegalStateException](await(sender.validateAndSend(messageType)))

          caught.getMessage shouldBe "Invalid request"
        }

        "return failed future when expected header X-Badge-ID is not present" in new SetUp {
          val importsMessageType: ImportsMessageType = messageType
          when(service.validate(body)).thenReturn(Future.successful(()))
          val headersWithOnlyXClientId = Map(HeaderNames.XClientId -> TestXClientId)
          when(data.headers).thenReturn(headersWithOnlyXClientId)
          val caught = intercept[IllegalStateException](await(sender.validateAndSend(messageType)))

          caught.getMessage shouldBe "Invalid request"
        }
      }

    }
  }
}
