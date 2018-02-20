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
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ValidateMovementController
import uk.gov.hmrc.customs.inventorylinking.imports.request.{Connector, OutgoingRequest, RequestInfo, RequestInfoGenerator}
import uk.gov.hmrc.customs.inventorylinking.imports.service.XmlValidationService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, NodeSeq}

class ValidateMovementControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  trait Setup {
    val serviceConfigProvider: ServiceConfigProvider = mock[ServiceConfigProvider]
    val body: Elem = <payload>payload</payload>
    val serviceConfig: ServiceConfig = ServiceConfig("/url/", Some("Bearer"), "environment")
    val requestInfoGenerator: RequestInfoGenerator = mock[RequestInfoGenerator]
    val mockXmlValidationService: XmlValidationService = mock[XmlValidationService]
    val conversationId: UUID = UUID.fromString("a26a559c-9a1c-42c5-a164-6508beea7749")
    val expectedConversationIdHeader: (String, String) = "X-Conversation-Id" -> conversationId.toString
    val correlationId = UUID.fromString("954e2369-3bfa-4aaa-a2a2-c4700e3f71ec")
    val requestDateTime = new DateTime(2017, 6, 8, 13, 55, 0, 0, DateTimeZone.UTC)
    val requestInfo = RequestInfo(conversationId, correlationId, requestDateTime)
    val connector: Connector = mock[Connector]

    val request: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(body)
    val controller: ValidateMovementController = new ValidateMovementController(connector, serviceConfigProvider, requestInfoGenerator, mockXmlValidationService)

    when(serviceConfigProvider.getConfig("imports")).thenReturn(serviceConfig)
    when(mockXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext])).thenReturn(())
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
        verify(mockXmlValidationService).validate(body)
        verifyNoMoreInteractions(mockXmlValidationService)
      }

      "return X-Conversation-Id header" in new Setup {
        stubConnectorReturnsResponseForPostedRequest(HttpResponse(ACCEPTED))

        val result = await(controller.postMessage("id").apply(request))

        result.header.headers should contain(expectedConversationIdHeader)
        verify(mockXmlValidationService).validate(body)
        verifyNoMoreInteractions(mockXmlValidationService)
      }
    }

    "backend service connector returns failure" should {
      "return 500 Internal Server Error" in new Setup {
        stubConnectorReturnsResponseForPostedRequest(Future.failed(TestData.emulatedServiceFailure))

        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        verify(mockXmlValidationService).validate(body)
        verifyNoMoreInteractions(mockXmlValidationService)
      }

      "return X-Conversation-Id header" in new Setup {
        stubConnectorReturnsResponseForPostedRequest(Future.failed(TestData.emulatedServiceFailure))

        val result = await(controller.postMessage("id").apply(request))

        result.header.headers should contain(expectedConversationIdHeader)
        verify(mockXmlValidationService).validate(body)
        verifyNoMoreInteractions(mockXmlValidationService)
      }
    }
  }
}
