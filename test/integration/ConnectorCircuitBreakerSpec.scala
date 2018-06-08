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

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.ImportsConnector
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.TestData
import util.TestData.mockUuidService
import util.XMLTestData.{ValidInventoryLinkingGoodsArrivalRequestXML, ValidInventoryLinkingMovementRequestXML}
import util.externalservices.InventoryLinkingImportsExternalServicesConfig.{GoodsArrivalConnectorContext, ValidateMovementConnectorContext}
import util.externalservices.InventoryLinkingImportsService

import scala.xml.NodeSeq


class ConnectorCircuitBreakerSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
  with InventoryLinkingImportsService with TableDrivenPropertyChecks {

  private val numberOfCallsToTriggerStateChange = 5
  private val unavailablePeriodDurationInMillis = 1000
  private val unstablePeriodDurationInMillis = 10000

  private val incomingBearerToken = "some_client's_bearer_token"
  private val incomingAuthToken = s"Bearer $incomingBearerToken"
  private val correlationId = UUID.randomUUID()
  private implicit val vpr = TestData.TestCspValidatedPayloadRequest

  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(incomingAuthToken)))

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder().configure(Map(
      "circuitBreaker.numberOfCallsToTriggerStateChange" -> numberOfCallsToTriggerStateChange,
      "circuitBreaker.unavailablePeriodDurationInMillis" -> unavailablePeriodDurationInMillis,
      "circuitBreaker.unstablePeriodDurationInMillis" -> unstablePeriodDurationInMillis,
      "microservice.services.validatemovement.host" -> Host,
      "microservice.services.validatemovement.port" -> Port,
      "microservice.services.validatemovement.context" -> ValidateMovementConnectorContext,
      "microservice.services.validatemovement.bearer-token" -> AuthToken,
      "microservice.services.goodsarrival.host" -> Host,
      "microservice.services.goodsarrival.port" -> Port,
      "microservice.services.goodsarrival.context" -> GoodsArrivalConnectorContext,
      "microservice.services.goodsarrival.bearer-token" -> AuthToken
    )).build()

  private lazy val connector = app.injector.instanceOf[ImportsConnector]

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def beforeEach(): Unit = {
    when(mockUuidService.uuid()).thenReturn(correlationId)
  }

  override protected def afterAll() {
    stopMockServer()
  }

  private val messageTypes = Table(("message type name", "xml", "message type", "url"),
    ("Goods Arrival", ValidInventoryLinkingGoodsArrivalRequestXML, new GoodsArrival(), GoodsArrivalConnectorContext),
    ("Validate Movement", ValidInventoryLinkingMovementRequestXML, new ValidateMovement(), ValidateMovementConnectorContext)
  )

  forAll(messageTypes) { case (messageTypeName, xml, messageType, url) =>

    "ImportsConnector" should {

      s"cause circuit breaker to trip after specified number of failures for $messageTypeName" in {
        setupBackendServiceToReturn(url, INTERNAL_SERVER_ERROR)

        1 to numberOfCallsToTriggerStateChange foreach { _ =>
          val k = intercept[Upstream5xxResponse](await(sendValidXml(messageType, xml)))
          k.reportAs shouldBe BAD_GATEWAY
        }

        1 to 3 foreach { _ =>
          val k = intercept[UnhealthyServiceException](await(sendValidXml(messageType, xml)))
          k.getMessage shouldBe "customs-inventory-linking-imports"
        }

        resetMockServer()
        setupBackendServiceToReturn(url, ACCEPTED)

        Thread.sleep(unavailablePeriodDurationInMillis)

        1 to 5 foreach { _ =>
          resetMockServer()
          setupBackendServiceToReturn(url, ACCEPTED)
          await(sendValidXml(messageType, xml))
          verifyImportsConnectorServiceWasCalledWith(url, requestBody = xml.toString())
        }
      }

    }
  }

  private def sendValidXml(importsMessageType: ImportsMessageType, xml:NodeSeq)(implicit vpr: ValidatedPayloadRequest[_]) = {
    connector.send(importsMessageType, xml, new DateTime(), correlationId)
  }

}
