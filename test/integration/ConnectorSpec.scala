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

package integration

import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ImportsConnector, OutgoingRequest}
import uk.gov.hmrc.http.{BadGatewayException, BadRequestException, NotFoundException, Upstream5xxResponse}
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.TestData.{body, requestInfo}
import util.externalservices.InventoryLinkingImportsExternalServicesConfig.{goodsArrivalConnectorContext, validateMovementConnectorContext}
import util.externalservices.InventoryLinkingImportsService


class ConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
  with InventoryLinkingImportsService with TableDrivenPropertyChecks {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder().configure(Map(
      "microservice.services.validatemovement.host" -> Host,
      "microservice.services.validatemovement.port" -> Port,
      "microservice.services.validatemovement.context" -> validateMovementConnectorContext,
      "microservice.services.validatemovement.bearer-token" -> AuthToken,
      "microservice.services.goodsarrival.host" -> Host,
      "microservice.services.goodsarrival.port" -> Port,
      "microservice.services.goodsarrival.context" -> goodsArrivalConnectorContext,
      "microservice.services.goodsarrival.bearer-token" -> AuthToken
    )).build()

  private lazy val connector = app.injector.instanceOf[ImportsConnector]
  private lazy val serviceConfigProvider = app.injector.instanceOf[ServiceConfigProvider]
  private val validateMovementRequest = OutgoingRequest(serviceConfigProvider.getConfig("validatemovement"), body, requestInfo)
  private val goodsArrivalRequest = OutgoingRequest(serviceConfigProvider.getConfig("goodsarrival"), body, requestInfo)

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  private val messageTypes = Table(("Message Type", "request", "url"),
    ("Goods Arrival", goodsArrivalRequest, goodsArrivalConnectorContext),
    ("Validate Movement", validateMovementRequest, validateMovementConnectorContext)
  )

  forAll(messageTypes) { case (messageType, request, url) =>

    "ImportsConnector" should {

      s"make a correct request for $messageType" in {
        setupBackendServiceToReturn(url, ACCEPTED)

        await(connector.post(request))

        verifyImportsConnectorServiceWasCalledWith(url, requestBody = "<payload>payload</payload>")
      }

      s"return a failed future when $messageType service returns 404" in {
        setupBackendServiceToReturn(url, NOT_FOUND)

        intercept[RuntimeException](await(connector.post(request))).getCause.getClass shouldBe classOf[NotFoundException]
      }

      s"return a failed future when $messageType service returns 400" in {
        setupBackendServiceToReturn(url, BAD_REQUEST)

        intercept[RuntimeException](await(connector.post(request))).getCause.getClass shouldBe classOf[BadRequestException]
      }

      s"return a failed future when $messageType service returns 500" in {
        setupBackendServiceToReturn(url, INTERNAL_SERVER_ERROR)

        intercept[Upstream5xxResponse](await(connector.post(request)))
      }

      s"return a failed future when failed to connect the $messageType service" in {
        stopMockServer()

        intercept[RuntimeException](await(connector.post(request))).getCause.getClass shouldBe classOf[BadGatewayException]

        startMockServer()
      }

    }
  }
}
