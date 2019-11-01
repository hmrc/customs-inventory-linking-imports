/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.ImportsConnector
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsCircuitBreakerConfig, SeqOfHeader}
import uk.gov.hmrc.customs.inventorylinking.imports.services.ImportsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import unit.logging.StubImportsLogger
import util.TestData._

import scala.concurrent.{ExecutionContext, Future}


class ImportsConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val stubImportsLogger = new StubImportsLogger(mock[CdsLogger])
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]
  private val mockImportsConfigService = mock[ImportsConfigService]
  private val mockImportsCircuitBreakerConfig = mock[ImportsCircuitBreakerConfig]
  private val mockResponse = mock[HttpResponse]
  private implicit val ec = Helpers.stubControllerComponents().executionContext
  
  private val connector = new ImportsConnector(mockWsPost, stubImportsLogger, mockServiceConfigProvider, mockImportsConfigService)

  private val goodsArrivalConfig = ServiceConfig("some-ga-url", Some("ga-bearer-token"), "ga-default")

  private val xml = <xml></xml>
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpException = new NotFoundException("Emulated 404 response from a web call")
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

  override protected def beforeEach() {
    reset(mockWsPost, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("goodsarrival")).thenReturn(goodsArrivalConfig)
    when(mockImportsConfigService.importsCircuitBreakerConfig).thenReturn(mockImportsCircuitBreakerConfig)
    when(mockResponse.body).thenReturn("<foo/>")
  }

  private val year = 2017
  private val monthOfYear = 7
  private val dayOfMonth = 4
  private val hourOfDay = 13
  private val minuteOfHour = 45
  private val date = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, DateTimeZone.UTC)

  private val httpFormattedDate = "Tue, 04 Jul 2017 13:45:00 UTC"

  private val correlationId = UUID.randomUUID()

  "ImportsConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(mockResponse))

        awaitRequest

        verify(mockWsPost).POSTString(ameq(goodsArrivalConfig.url), anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass the xml in the body" in {
        returnResponseForRequest(Future.successful(mockResponse))

        awaitRequest

        verify(mockWsPost).POSTString(anyString, ameq(xml.toString()), any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "set the content type header" in {
        returnResponseForRequest(Future.successful(mockResponse))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain(HeaderNames.CONTENT_TYPE -> s"${MimeTypes.XML}; charset=UTF-8")
      }

      "set the accept header" in {
        returnResponseForRequest(Future.successful(mockResponse))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain(HeaderNames.ACCEPT -> MimeTypes.XML)
      }

      "set the date header" in {
        returnResponseForRequest(Future.successful(mockResponse))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain(HeaderNames.DATE -> httpFormattedDate)
      }

      "set the X-Forwarded-Host header" in {
        returnResponseForRequest(Future.successful(mockResponse))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain(HeaderNames.X_FORWARDED_HOST -> "MDTP")
      }

      "set the X-Correlation-Id header" in {
        returnResponseForRequest(Future.successful(mockResponse))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain("X-Correlation-ID" -> correlationId.toString)
      }

      "set the X-Conversation-Id header" in {
        returnResponseForRequest(Future.successful(mockResponse))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain("X-Conversation-ID" -> ConversationIdValue)
      }

      "prefix the config key with the prefix if passed" in {
        returnResponseForRequest(Future.successful(mockResponse))

        await(connector.send(new GoodsArrival(), xml, date, correlationId))

        verify(mockServiceConfigProvider).getConfig("goodsarrival")
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when MDG call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(emulatedServiceFailure))

        val caught = intercept[EmulatedServiceFailure] {
          awaitRequest
        }
        caught shouldBe emulatedServiceFailure
      }

      "wrap an underlying error when MDG call fails with an http exception" in {
        returnResponseForRequest(Future.failed(httpException))

        val caught = intercept[RuntimeException] {
          awaitRequest
        }
        caught.getCause shouldBe httpException
      }
    }

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("goodsarrival")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          awaitRequest
        }
        caught.getMessage shouldBe "config not found"
      }
    }
  }

  private def awaitRequest = {
    await(connector.send(new GoodsArrival(), xml, date, correlationId))
  }

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]) = {
    when(mockWsPost.POSTString(anyString, anyString, any[SeqOfHeader])(
      any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }

}
