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
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.{GoodsArrivalController, ValidateMovementController}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{MessageSender, RequestInfoGenerator}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.Headers._
import util.TestData._

import scala.concurrent.Future

class ImportsControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with TableDrivenPropertyChecks with BeforeAndAfterEach {

  val serviceConfigProvider: ServiceConfigProvider = mock[ServiceConfigProvider]
  val requestInfoGenerator: RequestInfoGenerator = mock[RequestInfoGenerator]
  val clientId = "clientId"
  val badgeIdentifier = "badge"
  val messageSender: MessageSender = mock[MessageSender]

  val request: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(body).
    withHeaders(xClientIdName -> clientId.toString, xBadgeIdentifierName -> badgeIdentifier)

  val validateMovementController: ValidateMovementController = new ValidateMovementController(serviceConfigProvider, requestInfoGenerator, messageSender)
  val goodsArrivalController: GoodsArrivalController = new GoodsArrivalController(serviceConfigProvider, requestInfoGenerator, messageSender)

  override protected def beforeEach() {
    reset(serviceConfigProvider, requestInfoGenerator)
    when(requestInfoGenerator.newRequestInfo).thenReturn(requestInfo)
    when(serviceConfigProvider.getConfig("validatemovement")).thenReturn(serviceConfig)
    when(serviceConfigProvider.getConfig("goodsarrival")).thenReturn(serviceConfig)
  }


  private val controllers = Table(("controller name", "controller post"),
    ("Goods Arrival", goodsArrivalController.post("id")),
    ("Validate Movement", validateMovementController.post("id"))
  )

  forAll(controllers) { case (controllerName, controller) =>

    "POST valid declaration" when {
      "message is sent successfully" should {
        s"return 202 ACCEPTED for $controllerName" in {
          when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
            thenReturn(Future.successful(HttpResponse(ACCEPTED)))

          val result = await(controller.apply(request))

          status(result) shouldBe ACCEPTED
        }

        s"return X-Conversation-Id header for $controllerName" in {
          when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
            thenReturn(Future.successful(HttpResponse(ACCEPTED)))

          val result = await(controller.apply(request))

          result.header.headers should contain(conversationIdHeader)
        }
      }

      s"message fails due to backend service error for $controllerName" should {
        "return 500 Internal Server Error" in {
          when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
            thenReturn(Future.failed(emulatedServiceFailure))

          val result = await(controller.apply(request))

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

        s"return X-Conversation-Id header for $controllerName" in {
          when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
            thenReturn(Future.failed(emulatedServiceFailure))

          val result = await(controller.apply(request))

          result.header.headers should contain(conversationIdHeader)
        }
      }
    }

    s"POST invalid declaration for $controllerName" should {
      "return bad request" in {
        when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
          thenReturn(Future.failed(new SAXException()))

        val result = await(controller.apply(request))

        status(result) shouldBe BAD_REQUEST
      }
    }

  }

}
