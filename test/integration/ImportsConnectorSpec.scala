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

import org.joda.time.DateTime
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ImportsConnector, Non2xxResponseException}
import uk.gov.hmrc.customs.inventorylinking.imports.model.GoodsArrival
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.http.{Authorization, _}
import util.ExternalServicesConfig.{AuthToken, Host, Port}

import util.TestData
import util.XMLTestData.ValidInventoryLinkingMovementRequestXML
import util.externalservices.InventoryLinkingImportsExternalServicesConfig._
import util.externalservices.{ApiSubscriptionFieldsService, InventoryLinkingImportsService}

import java.util.UUID
import scala.xml.NodeSeq

class ImportsConnectorSpec extends IntegrationTestSpec with InventoryLinkingImportsService with GuiceOneAppPerSuite with MockitoSugar
  with ApiSubscriptionFieldsService with TableDrivenPropertyChecks {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder().configure(Map(
      "microservice.services.goodsarrival.host" -> Host,
      "microservice.services.goodsarrival.port" -> Port,
      "microservice.services.goodsarrival.context" -> GoodsArrivalConnectorContext,
      "microservice.services.goodsarrival.bearer-token" -> AuthToken,
      "metrics.enabled" -> false
    )).build()

  private lazy val connector = app.injector.instanceOf[ImportsConnector]

  private val incomingBearerToken = "some_client's_bearer_token"
  private val incomingAuthToken = s"Bearer $incomingBearerToken"
  private val correlationId = UUID.randomUUID()
  private implicit val vpr = TestData.TestCspValidatedPayloadRequest

  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(incomingAuthToken)))

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

    "ImportsConnector" should {

      "make a correct request" in {
        startImportsService(GoodsArrivalConnectorContext)

        await(sendValidXml(ValidInventoryLinkingMovementRequestXML))

        verifyImportsConnectorServiceWasCalledWith(GoodsArrivalConnectorContext, ValidInventoryLinkingMovementRequestXML.toString())
      }

      "return a failed future when service returns 403" in {
        startBackendService(FORBIDDEN)
        checkCorrectExceptionStatus(FORBIDDEN)
      }
      
      "return a failed future when service returns 404" in {
        startBackendService(NOT_FOUND)
        checkCorrectExceptionStatus(NOT_FOUND)
      }

      "return a failed future when service returns 400" in {
        startBackendService(BAD_REQUEST)
        checkCorrectExceptionStatus(BAD_REQUEST)
      }

      "return a failed future when service returns 500" in {
        startBackendService(INTERNAL_SERVER_ERROR)
        checkCorrectExceptionStatus(INTERNAL_SERVER_ERROR)
      }

      "return a failed future when connection with backend service fails" in {
        stopMockServer()

        intercept[BadGatewayException](await(sendValidXml(ValidInventoryLinkingMovementRequestXML)))

        startMockServer()
      }
  }

  private def startBackendService(status: Int) =
    setupBackendServiceToReturn(GoodsArrivalConnectorContext, status)

  private def sendValidXml(xml:NodeSeq)(implicit vpr: ValidatedPayloadRequest[_]) =
    connector.send(new GoodsArrival(), xml, new DateTime(), correlationId)

  private def checkCorrectExceptionStatus(status: Int): Unit = {
    val ex = intercept[Non2xxResponseException](await(sendValidXml(ValidInventoryLinkingMovementRequestXML)))
    ex.responseCode shouldBe status
  }
}
