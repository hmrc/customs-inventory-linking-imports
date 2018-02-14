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
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import scala.concurrent.{ExecutionContext, Future}
import play.api.http.Status.ACCEPTED

class InventoryLinkingImportsConnectorSpec extends UnitSpec with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val validMessage = <importMovement></importMovement>
  private val serviceConfig = ServiceConfig("the-url", Some("bearerToken"), "default")

  "sendInventoryLinkingMessage" when {
    "service returns an HTTP error" should {
      "return failed future with exception wrapped" in {
        val httpException = new NotFoundException("not found")
        val wsHttp = mock[WSHttp]
        val config = mock[ServiceConfigProvider]

        when(config.getConfig("inventory-linking-imports")).thenReturn(serviceConfig)

        when(wsHttp.POSTString(meq(serviceConfig.url), anyString, any[Seq[(String, String)]])
        (any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])).
          thenReturn(Future.failed(httpException))

        val connector = new InventoryLinkingImportsConnector(wsHttp, config)

        intercept[RuntimeException](await(connector.sendValidateMovementMessage(validMessage))).getCause shouldBe httpException
      }
    }

    "service returns Accepted" should {
      "return successful future" in {
        val wsHttp = mock[WSHttp]
        val config = mock[ServiceConfigProvider]

        when(config.getConfig("inventory-linking-imports")).thenReturn(serviceConfig)

        val acceptedResponse = HttpResponse(ACCEPTED)
        when(wsHttp.POSTString(meq(serviceConfig.url), meq(validMessage.toString()), any[Seq[(String, String)]])
        (any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])).
          thenReturn(Future.successful(acceptedResponse))

        val connector = new InventoryLinkingImportsConnector(wsHttp, config)

        val result = await(connector.sendValidateMovementMessage(validMessage))

        result shouldBe acceptedResponse
      }
    }
  }
}
