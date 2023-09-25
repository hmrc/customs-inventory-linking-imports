/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.CustomsMetricsConnector
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.http._
import util.VerifyLogging.verifyImportsLoggerError
import util.CustomsMetricsTestData.ValidCustomsMetricsRequest
import util.ExternalServicesConfig.{Host, Port}
import util.TestData
import util.externalservices.{CustomsMetricsService, InventoryLinkingImportsExternalServicesConfig}

class CustomsMetricsConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
 with CustomsMetricsService {

  private lazy val connector = app.injector.instanceOf[CustomsMetricsConnector]

  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest
  private implicit val mockImportsLogger: ImportsLogger = mock[ImportsLogger]

  def localhostString: String = {
    if (System.getenv("HOME") == "/home/jenkins") "127.0.0.1" else "0:0:0:0:0:0:0:1"
  }

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(IntegrationTestModule(mockImportsLogger).asGuiceableModule)).configure(Map(
      "microservice.services.customs-declarations-metrics.host" -> Host,
      "microservice.services.customs-declarations-metrics.port" -> Port,
      "microservice.services.customs-declarations-metrics.context" -> InventoryLinkingImportsExternalServicesConfig.CustomsMetricsContext,
      "metrics.enabled" -> false
    )).build()

  "MetricsConnector" should {

    "make a correct request" in {
      setupCustomsMetricsServiceToReturn()

      val response: Unit = await(sendValidRequest())

      response shouldBe
      verifyCustomsMetricsServiceWasCalledWith(ValidCustomsMetricsRequest)
    }

    "return a failed future when external service returns 404" in {
      setupCustomsMetricsServiceToReturn(NOT_FOUND)
      await(sendValidRequest()) shouldBe
      verifyImportsLoggerError("Call to customs metrics failed. url=http://localhost:11111/log-times, status=404, error=received a non 2XX response")
    }

    "return a failed future when external service returns 400" in {
      setupCustomsMetricsServiceToReturn(BAD_REQUEST)

      await(sendValidRequest()) shouldBe
      verifyImportsLoggerError("Call to customs metrics failed. url=http://localhost:11111/log-times, status=400, error=received a non 2XX response")
    }

    "return a failed future when external service returns 500" in {
      setupCustomsMetricsServiceToReturn(INTERNAL_SERVER_ERROR)

      await(sendValidRequest()) shouldBe
      verifyImportsLoggerError("Call to customs metrics failed. url=http://localhost:11111/log-times, status=500, error=received a non 2XX response")
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()

      intercept[RuntimeException](await(sendValidRequest())).getCause.getClass shouldBe classOf[BadGatewayException]
      //This seems to fail in local on MAC and pass on ubuntu due to how localhost ip is mapped
      verifyImportsLoggerError(s"Call to customs metrics failed. url=http://localhost:11111/log-times, status=502, error=POST of 'http://localhost:11111/log-times' failed. Caused by: 'Connection refused: localhost/$localhostString:11111'")

      startMockServer()
    }
  }

  private def sendValidRequest() = {
    connector.post(ValidCustomsMetricsRequest)
  }
  
}
