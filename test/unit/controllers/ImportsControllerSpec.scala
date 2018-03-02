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

package unit.controllers

import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.xml.sax.SAXException
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.http.MimeTypes
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.{GoodsArrivalController, ValidateMovementController}
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ValidateMovement}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{MessageSender, RequestInfoGenerator}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._

import scala.concurrent.Future

class ImportsControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with TableDrivenPropertyChecks with BeforeAndAfterEach {

  val serviceConfigProvider: ServiceConfigProvider = mock[ServiceConfigProvider]
  val requestInfoGenerator: RequestInfoGenerator = mock[RequestInfoGenerator]
  val clientId = "8e1043ef-fb32-4e90-9682-a8ff4a07228a"
  val badgeIdentifier = "badge"
  val messageSender: MessageSender = mock[MessageSender]

  val request: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(body).
    withHeaders(
      ACCEPT -> AcceptHeaderValue,
      CONTENT_TYPE -> MimeTypes.XML,
      XClientIdHeaderName -> clientId.toString,
      XBadgeIdentifierHeaderName -> badgeIdentifier)

  val logger = mock[CdsLogger]
  val validateMovementController: ValidateMovementController = new ValidateMovementController(requestInfoGenerator, messageSender, logger)
  val goodsArrivalController: GoodsArrivalController = new GoodsArrivalController(requestInfoGenerator, messageSender, logger)

  override protected def beforeEach() {
    reset(serviceConfigProvider, requestInfoGenerator)
    when(requestInfoGenerator.newRequestInfo).thenReturn(requestInfo)
  }


  private val controllers = Table(("Message Type Name", "Imports Message Type", "controller post"),
    ("Goods Arrival", GoodsArrival, goodsArrivalController.post()),
    ("Validate Movement", ValidateMovement, validateMovementController.post())
  )

  forAll(controllers) { case (messageTypeName, importsMessageType,  controller) =>

    "POST valid declaration" when {
      "message is sent successfully" should {
        s"return 202 ACCEPTED for $messageTypeName" in {
          when(messageSender.send(importsMessageType, body, requestInfo, request.headers.toSimpleMap)).
            thenReturn(Future.successful(HttpResponse(ACCEPTED)))

          val result = await(controller.apply(request))

          status(result) shouldBe ACCEPTED
        }

        s"return X-Conversation-Id header for $messageTypeName" in {
          when(messageSender.send(importsMessageType, body, requestInfo, request.headers.toSimpleMap)).
            thenReturn(Future.successful(HttpResponse(ACCEPTED)))

          val result = await(controller.apply(request))

          result.header.headers should contain(XConversationIdHeader)
        }
      }

      s"message fails due to backend service error for $messageTypeName" should {
        "return 500 Internal Server Error" in {
          when(messageSender.send(importsMessageType, body, requestInfo, request.headers.toSimpleMap)).
            thenReturn(Future.failed(emulatedServiceFailure))

          val result = await(controller.apply(request))

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

        s"return X-Conversation-Id header for $messageTypeName" in {
          when(messageSender.send(importsMessageType, body, requestInfo, request.headers.toSimpleMap)).
            thenReturn(Future.failed(emulatedServiceFailure))

          val result = await(controller.apply(request))

          result.header.headers should contain(XConversationIdHeader)
        }
      }
    }

    s"POST invalid declaration for $messageTypeName" should {
      "return bad request" in {
        when(messageSender.send(importsMessageType, body, requestInfo, request.headers.toSimpleMap)).
          thenReturn(Future.failed(new SAXException()))

        val result = await(controller.apply(request))

        status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
