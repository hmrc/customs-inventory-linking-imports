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

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers._
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.inventorylinking.imports.model.ApiSubscriptionFieldsResponse
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.http._
import util.ExternalServicesConfig.{Host, Port}
import util.TestData._
import util._
import util.externalservices.{ApiSubscriptionFieldsService, InventoryLinkingImportsExternalServicesConfig}

import scala.concurrent.Future

class ApiSubscriptionFieldsConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterAll with ApiSubscriptionFieldsService with ApiSubscriptionFieldsTestData {

  private lazy val connector = app.injector.instanceOf[ApiSubscriptionFieldsConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest

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
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "microservice.services.api-subscription-fields.host" -> Host,
      "microservice.services.api-subscription-fields.port" -> Port,
      "microservice.services.api-subscription-fields.context" -> InventoryLinkingImportsExternalServicesConfig.ApiSubscriptionFieldsContext,
      "metrics.enabled" -> false
    )).build()

  "ApiSubscriptionFieldsConnector" should {

    "return a ApiSubscriptionFieldsResponse when api subscription endpoint call is successful" in {
      setupGetSubscriptionFieldsToReturn()

      val response = await(getApiSubscriptionFields)

      response shouldBe apiSubscriptionFieldsResponse
      verifyGetSubscriptionFieldsCalled()
    }

    "correctly propagate NotFoundException when external service returns 404" in {
      setupGetSubscriptionFieldsToReturn(NOT_FOUND)

      checkCorrectExceptionStatus(NOT_FOUND)
      verifyGetSubscriptionFieldsCalled()
    }

    "correctly propagate BadRequestException when external service returns 400" in {
      setupGetSubscriptionFieldsToReturn(BAD_REQUEST)
      checkCorrectExceptionStatus(BAD_REQUEST)
      verifyGetSubscriptionFieldsCalled()
    }

    "correctly propagate Upstream4xxResponse when external service returns any 4xx response other than 400" in {
      setupGetSubscriptionFieldsToReturn(FORBIDDEN)
      checkCorrectExceptionStatus(FORBIDDEN)
      verifyGetSubscriptionFieldsCalled()
    }

    "correctly propagate Upstream5xxResponse when external service returns 500" in {
      setupGetSubscriptionFieldsToReturn(INTERNAL_SERVER_ERROR)
      checkCorrectExceptionStatus(INTERNAL_SERVER_ERROR)
      verifyGetSubscriptionFieldsCalled()
    }

    "correctly propagate Exception when failed to connect the external service" in {
      stopMockServer()
      intercept[RuntimeException](await(getApiSubscriptionFields)).getCause.getClass shouldBe classOf[BadGatewayException]
      startMockServer()
    }

  }

  private def getApiSubscriptionFields(implicit vpr: ValidatedPayloadRequest[_]): Future[ApiSubscriptionFieldsResponse] = {
    connector.getSubscriptionFields(apiSubscriptionKey)
  }

  private def checkCorrectExceptionStatus(status: Int): Unit = {
    val ex = intercept[UpstreamErrorResponse](await(getApiSubscriptionFields))
    ex.statusCode shouldBe status
  }
}
