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
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsXml
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.inventorylinking.imports.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ImportsConnector, Non2xxResponseException}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsCircuitBreakerConfig}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{DateTimeService, ImportsConfigService}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpAuditing, HttpClientV2Provider}
import util.ExternalServicesConfig.{Host, Port}
import util.TestData._
import util.{UnitSpec, VerifyLogging}
import util.externalservices.InventoryLinkingImportsExternalServicesConfig.GoodsArrivalConnectorContext

import java.net.SocketException
import java.time._
import java.time.format.DateTimeFormatter
import java.util.UUID


class ImportsConnectorSpec extends UnitSpec
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually
  with WireMockSupport
  with HttpClientV2Support {

  private val mockLogger = mock[ImportsLogger]
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]
  private val mockImportsConfigService = mock[ImportsConfigService]
  private val mockImportsCircuitBreakerConfig = mock[ImportsCircuitBreakerConfig]
  private val mockResponse = mock[HttpResponse]
  private val mockAuditConnector = mock[AuditConnector]

  lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.http.router" -> "definition.Routes",
      "application.logger.name" -> "customs-inventory-linking-imports",
      "appName" -> "customs-inventory-linking-imports",
      "appUrl" -> "http://customs-inventory-linking-imports-host",
      "microservice.services.goodsarrival.context" -> GoodsArrivalConnectorContext,
      "microservice.services.goodsarrival.host" -> Host,
      "microservice.services.goodsarrival.port" -> Port,
      "microservice.services.goodsarrival.bearer-token" -> "v1-ga-bearer-token",
      "microservice.services.v2.goodsarrival.host" -> Host,
      "microservice.services.v2.goodsarrival.port" -> Port,
      "microservice.services.v2.goodsarrival.bearer-token" -> "v2-ga-bearer-token",
      "metrics.enabled" -> false
    ).overrides(
      bind[HttpAuditing].to[DefaultHttpAuditing],
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[String].qualifiedWith("appName").toInstance("customs-inventory-linking-imports"),
      bind[HttpClientV2].toProvider[HttpClientV2Provider],
      bind[ImportsLogger].toInstance(mockLogger),
      bind[ImportsConfigService].toInstance(mockImportsConfigService),
      bind[ServiceConfigProvider].toInstance(mockServiceConfigProvider),
      bind[ImportsCircuitBreakerConfig].toInstance(mockImportsCircuitBreakerConfig),
    )
    .build()

  private val connector: ImportsConnector = app.injector.instanceOf[ImportsConnector]

  private val goodsArrivalConfigv1 = ServiceConfig(s"$wireMockUrl$GoodsArrivalConnectorContext", Some("v1-ga-bearer-token"), "V1-ga-default")
  private val goodsArrivalConfigv2 = ServiceConfig(s"$wireMockUrl$GoodsArrivalConnectorContext", Some("v2-ga-bearer-token"), "V2-ga-default")

  private val xml = <xml></xml>
  private implicit val hc : HeaderCarrier = HeaderCarrier()

  override protected def beforeEach(): Unit = {
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
    when(mockServiceConfigProvider.getConfig("goodsarrival")).thenReturn(goodsArrivalConfigv1)
    when(mockServiceConfigProvider.getConfig("v2.goodsarrival")).thenReturn(goodsArrivalConfigv2)
    when(mockImportsConfigService.importsCircuitBreakerConfig).thenReturn(mockImportsCircuitBreakerConfig)
    when(mockResponse.body).thenReturn("<foo/>")
    when(mockResponse.status).thenReturn(OK)
  }

  private val date = LocalDateTime.now()

  private val utcDateFormat: DateTimeFormatter = new DateTimeService().utcFormattedDate

  private val httpFormattedDate = LocalDateTime.now().atOffset(ZoneOffset.UTC).format(utcDateFormat)

  private val correlationId = UUID.randomUUID()

  "ImportsConnector" can {

    "when making a successful request" should {

      "passing all headers and v1 config" in {

        implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

        wireMockServer.stubFor(post(urlEqualTo(GoodsArrivalConnectorContext))
          .withHeader(HeaderNames.AUTHORIZATION, equalTo(s"Bearer ${goodsArrivalConfigv1.bearerToken.get}"))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/xml; charset=UTF-8"))
          .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.XML))
          .withHeader(HeaderNames.DATE, equalTo(httpFormattedDate))
          .withHeader(HeaderNames.X_FORWARDED_HOST, equalTo("MDTP"))
          .withHeader("X-Correlation-ID", equalTo(correlationId.toString))
          .withRequestBody(equalTo(xml.toString()))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
        )
        val response = await(connector.send(new GoodsArrival(), xml, date, correlationId))

        response.status shouldBe OK
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(GoodsArrivalConnectorContext))
          .withHeader(HeaderNames.AUTHORIZATION, equalTo(s"Bearer ${goodsArrivalConfigv1.bearerToken.get}"))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/xml; charset=UTF-8"))
          .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.XML))
          .withHeader(HeaderNames.DATE, equalTo(httpFormattedDate))
          .withHeader(HeaderNames.X_FORWARDED_HOST, equalTo("MDTP"))
          .withHeader("X-Correlation-ID", equalTo(correlationId.toString))
          .withRequestBody(equalTo("<xml></xml>"))
        )
        VerifyLogging.verifyImportsLogger("debug","Sending request to backend. Url: http://localhost:6001/inventorylinkingimports/goodsarrivalnotification\nPayload: <xml></xml>")(mockLogger)
        VerifyLogging.verifyImportsLogger("debug","Response status 200 and response body <empty>")(mockLogger)
      }

      "passing all headers and v2 config" in {
        implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequestV2

        wireMockServer.stubFor(post(urlEqualTo(GoodsArrivalConnectorContext))
          .withHeader(HeaderNames.AUTHORIZATION, equalTo(s"Bearer ${goodsArrivalConfigv2.bearerToken.get}"))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/xml; charset=UTF-8"))
          .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.XML))
          .withHeader(HeaderNames.DATE, equalTo(httpFormattedDate))
          .withHeader(HeaderNames.X_FORWARDED_HOST, equalTo("MDTP"))
          .withHeader("X-Correlation-ID", equalTo(correlationId.toString))
          .withRequestBody(equalTo(xml.toString()))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
        )
        val response = await(connector.send(new GoodsArrival(), xml, date, correlationId))

        response.status shouldBe OK
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(GoodsArrivalConnectorContext))
          .withHeader(HeaderNames.AUTHORIZATION, equalTo(s"Bearer ${goodsArrivalConfigv2.bearerToken.get}"))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/xml; charset=UTF-8"))
          .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.XML))
          .withHeader(HeaderNames.DATE, equalTo(httpFormattedDate))
          .withHeader(HeaderNames.X_FORWARDED_HOST, equalTo("MDTP"))
          .withHeader("X-Correlation-ID", equalTo(correlationId.toString))
          .withRequestBody(equalTo("<xml></xml>"))
      )
      }
    }
    "when making an failing request" should {
      "propagate an underlying error when MDG call fails with a non-http exception" in {

        implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

        wireMockServer.stubFor(post(urlEqualTo(GoodsArrivalConnectorContext))
          .willReturn(
            aResponse()
              .withFault(Fault.CONNECTION_RESET_BY_PEER)
          ))

        val caught = intercept[SocketException] {
          await(connector.send(new GoodsArrival(), xml, date, correlationId))
        }
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(GoodsArrivalConnectorContext)))
        caught shouldBe a[SocketException]
        caught.getMessage should include("Connection reset")
        VerifyLogging.verifyImportsLoggerThrowable("error","Call to backend failed. url=[http://localhost:6001/inventorylinkingimports/goodsarrivalnotification]")(mockLogger)
      }

      "wrap an underlying error when call fails with an http exception when no response body is present" in {

        implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

        wireMockServer.stubFor(post(urlEqualTo(GoodsArrivalConnectorContext))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          ))

        val caught = intercept[Non2xxResponseException] {
          await(connector.send(new GoodsArrival(), xml, date, correlationId))
        }
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(GoodsArrivalConnectorContext)))
        caught shouldBe a[Non2xxResponseException]
        caught.getMessage shouldBe s"Call to Inventory Linking Imports backend failed. Status=[$NOT_FOUND] url=[$wireMockUrl$GoodsArrivalConnectorContext] response body=[<empty>]"
      }

      "wrap an underlying error when call fails with an http exception when a response body is present" in {

        implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

        wireMockServer.stubFor(post(urlEqualTo(GoodsArrivalConnectorContext))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody("NOT_FOUND error")
          ))

        val caught = intercept[Non2xxResponseException] {
          await(connector.send(new GoodsArrival(), xml, date, correlationId))
        }
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(GoodsArrivalConnectorContext)))
        caught shouldBe a[Non2xxResponseException]
        caught.getMessage shouldBe s"Call to Inventory Linking Imports backend failed. Status=[$NOT_FOUND] url=[$wireMockUrl$GoodsArrivalConnectorContext] response body=[NOT_FOUND error]"
      }
    }

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("goodsarrival")).thenReturn(null)

        implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

        wireMockServer.stubFor(post(urlEqualTo(GoodsArrivalConnectorContext))
          .willReturn(
            aResponse()
          ))
        val caught = intercept[IllegalArgumentException] {
          await(connector.send(new GoodsArrival(), xml, date, correlationId))
        }
        caught.getMessage shouldBe "config not found"
      }

      "throw an exception when no bearer token value is found in config" in {
        val mockServiceConfig = mock[ServiceConfig]
        when(mockServiceConfig.bearerToken).thenReturn(None)
        when(mockServiceConfigProvider.getConfig("goodsarrival")).thenReturn(mockServiceConfig)

        implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

        wireMockServer.stubFor(post(urlEqualTo(GoodsArrivalConnectorContext))
          .willReturn(
            aResponse()
          ))

        val caught = intercept[IllegalStateException] {
          await(connector.send(new GoodsArrival(), xml, date, correlationId))
        }
        caught.getMessage shouldBe "no bearer token was found in config"
      }
    }
  }
}
