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

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ValidateMovementController
import uk.gov.hmrc.customs.inventorylinking.imports.request.{Connector, OutgoingRequest, RequestInfo, RequestInfoGenerator}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData

import scala.concurrent.Future
import scala.xml.Elem

class ValidateMovementControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  trait Setup {
    private val serviceConfigProvider: ServiceConfigProvider = mock[ServiceConfigProvider]
    private val body: Elem = <payload>payload</payload>
    private val serviceConfig: ServiceConfig = ServiceConfig("/url/", Some("Bearer"), "environment")
    private val requestInfoGenerator: RequestInfoGenerator = mock[RequestInfoGenerator]
    val conversationId: UUID = UUID.fromString("a26a559c-9a1c-42c5-a164-6508beea7749")
    val expectedConversationIdHeader: (String, String) = "X-Conversation-Id" -> conversationId.toString
    private val correlationId = UUID.fromString("954e2369-3bfa-4aaa-a2a2-c4700e3f71ec")
    private val requestDateTime = new DateTime(2017, 6, 8, 13, 55, 0, 0, DateTimeZone.UTC)
    private val requestInfo = RequestInfo(conversationId, correlationId, requestDateTime)
    private val connector: Connector = mock[Connector]

    val request: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(body)
    val controller: ValidateMovementController = new ValidateMovementController(connector, serviceConfigProvider, requestInfoGenerator)

    when(serviceConfigProvider.getConfig("imports")).thenReturn(serviceConfig)

    when(requestInfoGenerator.newRequestInfo).thenReturn(requestInfo)

    def stubConnectorReturnsResponseForPostedRequest(response: Future[HttpResponse]): Unit ={
      val outgoingRequest: OutgoingRequest = OutgoingRequest(serviceConfig, body, requestInfo)

      when(connector.postRequest(outgoingRequest)).
        thenReturn(Future.successful(response))
    }
  }

  "POST valid declaration" when {
    "backend service connector returns ACCEPTED" should {
      "return 202 ACCEPTED" in new Setup {
        stubConnectorReturnsResponseForPostedRequest(HttpResponse(ACCEPTED))

        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe ACCEPTED
      }

      "return X-Conversation-Id header" in new Setup {
        stubConnectorReturnsResponseForPostedRequest(HttpResponse(ACCEPTED))

        val result = await(controller.postMessage("id").apply(request))

        result.header.headers should contain(expectedConversationIdHeader)
      }
    }

    "backend service connector returns failure" should {
      "return 500 Internal Server Error" in new Setup {
        stubConnectorReturnsResponseForPostedRequest(Future.failed(TestData.emulatedServiceFailure))

        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return X-Conversation-Id header" in new Setup {
        stubConnectorReturnsResponseForPostedRequest(Future.failed(TestData.emulatedServiceFailure))

        val result = await(controller.postMessage("id").apply(request))

        result.header.headers should contain(expectedConversationIdHeader)
      }
    }
  }
}
