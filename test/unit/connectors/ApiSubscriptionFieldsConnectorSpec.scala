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

package unit.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers.ACCEPT
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.ImportsConfig
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.services.ImportsConfigService
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClientV2Provider
import util.{ApiSubscriptionFieldsTestData, TestData, UnitSpec}

import java.net.SocketException
import scala.concurrent.ExecutionContext

class ApiSubscriptionFieldsConnectorSpec extends UnitSpec
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually
  with ApiSubscriptionFieldsTestData
  with WireMockSupport {

  private val mockLogger = mock[ImportsLogger]
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest

  private val mockImportsConfigService: ImportsConfigService = mock[ImportsConfigService]
  private val mockImportsConfig: ImportsConfig = mock[ImportsConfig]

  lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.http.router" -> "definition.Routes",
      "application.logger.name" -> "customs-inventory-linking-imports",
      "appName" -> "customs-inventory-linking-imports",
      "appUrl" -> "http://customs-inventory-linking-imports-host"
    ).overrides(
      bind[String].qualifiedWith("appName").toInstance("customs-inventory-linking-imports"),
      bind[HttpClientV2].toProvider[HttpClientV2Provider],
      bind[ImportsLogger].toInstance(mockLogger),
      bind[ImportsConfigService].toInstance(mockImportsConfigService)
    )
    .build()

  private val connector: ApiSubscriptionFieldsConnector = app.injector.instanceOf[ApiSubscriptionFieldsConnector]

  private val httpException = new NotFoundException("Emulated 404 response from a web call")

  // I think this should be correct but this is not the url being used (customs-declaration who also call this use this url)
  //  private val expectedUrl = s"$ApiSubscriptionFieldsContext/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0"

  private val expectedUrl = s"/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0"
  private val mockRequestBuilder = mock[RequestBuilder]

  override protected def beforeEach(): Unit = {
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
    when(mockImportsConfigService.importsConfig).thenReturn(mockImportsConfig)
    when(mockImportsConfig.apiSubscriptionFieldsBaseUrl).thenReturn(s"http://localhost:${wireMockServer.port()}")
  }

  "ApiSubscriptionFieldsConnector" can {
    "when making a successful request" should {
      "use the correct URL for valid path parameters and config" in {
        wireMockServer.stubFor(get(urlEqualTo(expectedUrl))
          .withHeader(ACCEPT, equalTo("*/*"))
          .willReturn(
            aResponse()
              .withBody(Json.stringify(Json.toJson(apiSubscriptionFieldsResponse)))
              .withStatus(OK)
          )
        )

        awaitRequest shouldBe apiSubscriptionFieldsResponse
        wireMockServer.verify(1, getRequestedFor(urlEqualTo(expectedUrl)))
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when api subscription fields call fails with a non-http exception" in {
        wireMockServer.stubFor(get(urlEqualTo(expectedUrl))
          .withHeader(ACCEPT, equalTo("*/*"))
          .willReturn(
            aResponse()
              .withFault(Fault.CONNECTION_RESET_BY_PEER)
          )
        )

        val caught = intercept[SocketException] {
          awaitRequest
        }
        wireMockServer.verify(1, getRequestedFor(urlEqualTo(expectedUrl)))
        caught shouldBe a[SocketException]
        caught.getMessage should include("Connection reset")
      }

      "wrap an underlying error when api subscription fields call fails with an http exception" in {

        wireMockServer.stubFor(get(urlEqualTo(expectedUrl))
          .withHeader(ACCEPT, equalTo("*/*"))
          .willReturn(
            aResponse()
              .withBody(Json.stringify(Json.toJson(apiSubscriptionFieldsResponse)))
              .withStatus(NOT_FOUND)
          )
        )
        val caught = intercept[UpstreamErrorResponse] {
          awaitRequest
        }

        wireMockServer.verify(1, getRequestedFor(urlEqualTo(expectedUrl)))
        caught.getMessage shouldBe s"""GET of 'http://localhost:6001/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0' returned 404. Response body: '{"fieldsId":"327d9145-4965-4d28-a2c5-39dedee50334","fields":{"authenticatedEori":"RASHADMUGHAL"}}'"""

      }
    }
  }

  private def awaitRequest = {
    await(connector.getSubscriptionFields(apiSubscriptionKey))
  }
}
