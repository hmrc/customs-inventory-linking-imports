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

import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.inventorylinking.imports.WSHttp
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.InventoryLinkingImportsConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.stubbing.OngoingStubbing
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}

import scala.concurrent.{ExecutionContext, Future}
import play.api.http.Status.ACCEPTED

import scala.xml.Elem

class InventoryLinkingImportsConnectorSpec extends UnitSpec with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait SetupConnector {
    val validMessage: Elem = <importMovement></importMovement>
    val serviceConfig = ServiceConfig("the-url", Some("bearerToken"), "default")
    val config: ServiceConfigProvider = mock[ServiceConfigProvider]
    val wsHttp: WSHttp = mock[WSHttp]
    val connector = new InventoryLinkingImportsConnector(wsHttp, config)

    when(config.getConfig("inventory-linking-imports")).thenReturn(serviceConfig)

    def stubHttpClientReturnsResponseForValidMessage(response: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] = {
      when(wsHttp.POSTString(meq(serviceConfig.url), meq(validMessage.toString()), any[Seq[(String, String)]])
      (any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])).
        thenReturn(response)
    }

    def sendValidMessageToConnector: HttpResponse = {
      await(connector.sendValidateMovementMessage(validMessage))
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
