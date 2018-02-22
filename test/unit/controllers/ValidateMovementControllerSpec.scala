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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.xml.sax
import org.xml.sax.SAXException
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ValidateMovementController
import uk.gov.hmrc.customs.inventorylinking.imports.request._
import uk.gov.hmrc.customs.inventorylinking.imports.service.XmlValidationService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.Headers._
import util.TestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class ValidateMovementControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  trait Setup {
    private val serviceConfigProvider = mock[ServiceConfigProvider]
    private val decoratedBody = <wrapped><payload>payload</payload></wrapped>
    private val requestInfoGenerator = mock[RequestInfoGenerator]
    val mockXmlValidationService: XmlValidationService = mock[XmlValidationService]
    private val clientId = "clientId"
    private val badgeIdentifier = "badge"
    private val connector = mock[Connector]
    private val decorator = mock[PayloadDecorator]

    val request: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(body).
      withHeaders(xClientIdName -> clientId.toString, xBadgeIdentifierName -> badgeIdentifier)

    val controller: ValidateMovementController = new ValidateMovementController(connector, serviceConfigProvider, requestInfoGenerator, mockXmlValidationService, decorator)
    when(serviceConfigProvider.getConfig("imports")).thenReturn(serviceConfig)
    when(requestInfoGenerator.newRequestInfo).thenReturn(requestInfo)
    when(decorator.wrap(body, requestInfo, clientId, badgeIdentifier)).thenReturn(decoratedBody)

    def stubXmlValidationReturnsSuccess(): Unit = {
      when(mockXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext])).thenReturn(body)
    }

    def stubConnectorReturnsResponseForPostedRequest(response: Future[HttpResponse]): Unit ={
      val outgoingRequest: OutgoingRequest = OutgoingRequest(serviceConfig, decoratedBody, requestInfo)

      when(connector.postRequest(outgoingRequest)).
        thenReturn(Future.successful(response))
    }
  }

  "POST valid declaration" when {
    "backend service connector returns ACCEPTED" should {
      "return 202 ACCEPTED" in new Setup {
        stubXmlValidationReturnsSuccess()
        stubConnectorReturnsResponseForPostedRequest(HttpResponse(ACCEPTED))

        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe ACCEPTED
      }

      "return X-Conversation-Id header" in new Setup {
        stubXmlValidationReturnsSuccess()
        stubConnectorReturnsResponseForPostedRequest(HttpResponse(ACCEPTED))

        val result = await(controller.postMessage("id").apply(request))

        result.header.headers should contain(conversationIdHeader)
      }
    }

    "backend service connector returns failure" should {
      "return 500 Internal Server Error" in new Setup {
        stubXmlValidationReturnsSuccess()
        stubConnectorReturnsResponseForPostedRequest(Future.failed(emulatedServiceFailure))

        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return X-Conversation-Id header" in new Setup {
        stubXmlValidationReturnsSuccess()
        stubConnectorReturnsResponseForPostedRequest(Future.failed(emulatedServiceFailure))

        val result = await(controller.postMessage("id").apply(request))

        result.header.headers should contain(conversationIdHeader)
      }
    }
  }

  "POST invalid declaration" should {
    "return bad request" in new Setup {
      when(mockXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext])).
              thenReturn(Future.failed(new sax.SAXException()))

      val result = await(controller.postMessage("id").apply(request))

      status(result) shouldBe BAD_REQUEST
    }
  }
}
