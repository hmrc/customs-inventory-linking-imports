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

package unit.connectors

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.customs.api.common.config.ServiceConfig
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ImportsConnector, OutgoingRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestDataWrapper
import uk.gov.hmrc.customs.inventorylinking.imports.services.WSHttp
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData
import util.TestData.requestDateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class ConnectorSpec extends UnitSpec with MockitoSugar {

  trait SetupConnector {
    private val serviceConfig = ServiceConfig("the-url", Some("bearerToken"), "default")
    private val wsHttp: WSHttp = mock[WSHttp]
    private val connector = new ImportsConnector(wsHttp, mock[ImportsLogger])
    private val validOutgoingMessage: Elem = <message></message>

    private implicit val rdWrapper = mock[RequestDataWrapper]

    private val request = OutgoingRequest(serviceConfig, validOutgoingMessage, rdWrapper)

    def stubHttpClientReturnsResponseForValidMessage(response: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] = {
      when(rdWrapper.dateTime).thenReturn(requestDateTime)
      when(wsHttp.POSTString(meq(serviceConfig.url), meq(validOutgoingMessage.toString()), any[Seq[(String, String)]])
      (any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])).
        thenReturn(response)
    }

    def sendValidMessageToConnector: HttpResponse = {
      await(connector.post(request))
    }
  }

  "sendInventoryLinkingMessage" when {

    "service returns an HTTP error" should {

      "return failed future with exception wrapped" in new SetupConnector {
        private val notFound = new NotFoundException("not found")

        stubHttpClientReturnsResponseForValidMessage(
          Future.failed(notFound))

        intercept[RuntimeException](sendValidMessageToConnector).getCause shouldBe notFound
      }
    }

    "service returns non HTTP error" should {
      "return failed future with exception" in new SetupConnector {
        stubHttpClientReturnsResponseForValidMessage(
          Future.failed(TestData.emulatedServiceFailure))

        intercept[TestData.EmulatedServiceFailure](sendValidMessageToConnector)
      }
    }

    "service returns Accepted" should {
      "return successful future" in new SetupConnector {
        private val accepted = HttpResponse(ACCEPTED)

        stubHttpClientReturnsResponseForValidMessage(
          Future.successful(accepted))

        private val result = sendValidMessageToConnector

        result shouldBe accepted
      }
    }
  }
}
