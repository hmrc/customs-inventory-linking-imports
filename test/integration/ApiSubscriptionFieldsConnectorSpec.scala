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

import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Headers}
import play.api.test.Helpers._
import play.api.{Application, mvc}
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.inventorylinking.imports.model.{RequestData, ValidatedRequest}
import uk.gov.hmrc.http._
import util.ApiSubscriptionFieldsTestData._
import util.ExternalServicesConfig.{Host, Port}
import util.TestData._
import util.externalservices.{ApiSubscriptionFieldsService, InventoryLinkingImportsExternalServicesConfig}

import scala.concurrent.Future

class ApiSubscriptionFieldsConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterAll with ApiSubscriptionFieldsService {

  private lazy val connector = app.injector.instanceOf[ApiSubscriptionFieldsConnector]

  private val request: mvc.Request[AnyContent] = mock[mvc.Request[AnyContent]]
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private lazy val requestData: RequestData = createRequestData
  private implicit lazy val rdWrapper: ValidatedRequest[AnyContent] = ValidatedRequest[AnyContent](requestData, request)

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().configure(Map(
      "microservice.services.api-subscription-fields.host" -> Host,
      "microservice.services.api-subscription-fields.port" -> Port,
      "microservice.services.api-subscription-fields.context" -> InventoryLinkingImportsExternalServicesConfig.ApiSubscriptionFieldsContext
    )).build()

  "ApiSubscriptionFieldsConnector" should {

    when(request.headers).thenReturn(Headers("X-Client-ID" -> TestXClientId))

    "make a correct request" in {
      setupGetSubscriptionFieldsToReturn()

      val response = await(getApiSubscriptionFields)

      response shouldBe FieldsId
      verifyGetSubscriptionFieldsCalled(rdWrapper.requestData.clientId)
    }

    "return a failed future when external service returns 404" in {
      setupGetSubscriptionFieldsToReturn(NOT_FOUND)

      intercept[RuntimeException](await(getApiSubscriptionFields)).getCause.getClass shouldBe classOf[NotFoundException]
    }

    "return a failed future when external service returns 400" in {
      setupGetSubscriptionFieldsToReturn(BAD_REQUEST)
      intercept[RuntimeException](await(getApiSubscriptionFields)).getCause.getClass shouldBe classOf[BadRequestException]
    }

    "return a failed future when external service returns any 4xx response other than 400" in {
      setupGetSubscriptionFieldsToReturn(FORBIDDEN)
      intercept[Upstream4xxResponse](await(getApiSubscriptionFields))
    }

    "return a failed future when external service returns 500" in {
      setupGetSubscriptionFieldsToReturn(INTERNAL_SERVER_ERROR)
      intercept[Upstream5xxResponse](await(getApiSubscriptionFields))
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      intercept[RuntimeException](await(getApiSubscriptionFields)).getCause.getClass shouldBe classOf[BadGatewayException]
      startMockServer()
    }

  }

  private def getApiSubscriptionFields: Future[UUID] = {
    connector.getClientSubscriptionId()
  }
}
