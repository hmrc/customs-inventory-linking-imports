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

import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.xml.sax.SAXException
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ValidateMovementController
import uk.gov.hmrc.customs.inventorylinking.imports.services.{RequestInfoGenerator, MessageSender}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.Headers._
import util.TestData._

import scala.concurrent.Future

class ValidateMovementControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  trait Setup {
    val serviceConfigProvider = mock[ServiceConfigProvider]
    private val requestInfoGenerator = mock[RequestInfoGenerator]
    private val clientId = "clientId"
    private val badgeIdentifier = "badge"
    val messageSender: MessageSender = mock[MessageSender]

    val request: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(body).
      withHeaders(xClientIdName -> clientId.toString, xBadgeIdentifierName -> badgeIdentifier)

    val controller: ValidateMovementController = new ValidateMovementController(serviceConfigProvider, requestInfoGenerator, messageSender)
    when(requestInfoGenerator.newRequestInfo).thenReturn(requestInfo)
  }

  trait ValidateMovementSetup extends Setup {
    when(serviceConfigProvider.getConfig("validatemovement")).thenReturn(serviceConfig)
  }

  "POST valid declaration" when {
    "message is sent successfully" should {
      "return 202 ACCEPTED" in new ValidateMovementSetup {
        when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
          thenReturn(Future.successful(HttpResponse(ACCEPTED)))

        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe ACCEPTED
      }

      "return X-Conversation-Id header" in new ValidateMovementSetup {
        when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
          thenReturn(Future.successful(HttpResponse(ACCEPTED)))

        val result = await(controller.postMessage("id").apply(request))

        result.header.headers should contain(conversationIdHeader)
      }
    }

    "message fails due to backend service error" should {
      "return 500 Internal Server Error" in new ValidateMovementSetup {
        when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
          thenReturn(Future.failed(emulatedServiceFailure))

        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return X-Conversation-Id header" in new ValidateMovementSetup {
        when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
          thenReturn(Future.failed(emulatedServiceFailure))

        val result = await(controller.postMessage("id").apply(request))

        result.header.headers should contain(conversationIdHeader)
      }
    }
  }

  "POST invalid declaration" should {
    "return bad request" in new ValidateMovementSetup {
      when(messageSender.send(body, requestInfo, request.headers.toSimpleMap, serviceConfig)).
        thenReturn(Future.failed(new SAXException()))

      val result = await(controller.postMessage("id").apply(request))

      status(result) shouldBe BAD_REQUEST
    }
  }
}
