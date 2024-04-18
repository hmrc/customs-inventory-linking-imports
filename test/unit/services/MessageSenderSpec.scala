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

package unit.services

import org.apache.pekko.pattern.CircuitBreakerOpenException

import java.time._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{atLeastOnce, verify, verifyNoMoreInteractions, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_GATEWAY, FORBIDDEN, NOT_FOUND}
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.Helpers
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ErrorResponse.{ErrorInternalServerError, ErrorPayloadForbidden, errorInternalServerError}
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ApiSubscriptionFieldsConnector, ImportsConnector}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders._
import uk.gov.hmrc.customs.inventorylinking.imports.services._
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}
import util.ApiSubscriptionFieldsTestData._
import util.TestData.{TestCspValidatedPayloadRequest, TestXmlPayload, _}
import util.{UnitSpec, VerifyLogging}

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.xml.NodeSeq


class MessageSenderSpec extends UnitSpec with Matchers with MockitoSugar with TableDrivenPropertyChecks {

  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private val dateTime = LocalDateTime.now()
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val expectedApiSubscriptionKeyV1 = ApiSubscriptionKey(clientId, "customs%2Finventory-linking-imports", VersionOne)
  private val expectedApiSubscriptionKeyV2 = expectedApiSubscriptionKeyV1.copy(version = VersionTwo)
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest
  private val wrappedValidXML = <wrapped></wrapped>
  private val errorResponseServiceUnavailable = errorInternalServerError("This service is currently unavailable")
  private val errorResponseMissingEori = errorInternalServerError("Missing authenticated eori in service lookup")

  trait SetUp {
    protected val importsMessageType: ImportsMessageType = new GoodsArrival()
    implicit protected val mockLogger: ImportsLogger = mock[ImportsLogger]
    protected val mockImportsConnector: ImportsConnector = mock[ImportsConnector]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockPayloadDecorator: PayloadDecorator = mock[PayloadDecorator]
    protected val mockDateTimeProvider: DateTimeService = mock[DateTimeService]
    protected val mockConfigService: ImportsConfigService = mock[ImportsConfigService]
    protected val importsConfig: ImportsConfig = mock[ImportsConfig]
    protected val mockHttpResponse: HttpResponse = mock[HttpResponse]

    protected lazy val service: MessageSender = new MessageSender(mockApiSubscriptionFieldsConnector, mockPayloadDecorator, mockImportsConnector,
      mockDateTimeProvider, stubUniqueIdsService, mockLogger)

    protected def callSend(vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest, hc: HeaderCarrier = headerCarrier): Either[Result, Unit] = {
      await(service.send(importsMessageType)(vpr, hc))
    }

    when(mockPayloadDecorator.wrap(
      meq(TestXmlPayload),
      meq(apiSubscriptionFieldsResponse),
      any[Option[String]].asInstanceOf[Option[CorrelationIdHeader]],
      meq(importsMessageType.wrapperRootElementLabel),
      any[LocalDateTime],
      meq(CorrelationIdUuid))(any[ValidatedPayloadRequest[_]])
    ).thenReturn(wrappedValidXML)

    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockConfigService.importsConfig).thenReturn(importsConfig)
    when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], meq(dateTime), any[UUID])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(mockHttpResponse)
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
  }

  "MessageSender" should {

    "send transformed xml to connector" in new SetUp() {
      callSend() shouldBe Right(())

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
      verify(mockImportsConnector).send(meq(importsMessageType), meq(wrappedValidXML), any[LocalDateTime], any[UUID])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "ensure that correct version is used in call to subscription service" in new SetUp() {
      callSend(vpr = TestCspValidatedPayloadRequestV2) shouldBe Right(())

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(expectedApiSubscriptionKeyV2)(TestCspValidatedPayloadRequestV2, headerCarrier)
      verify(mockImportsConnector).send(meq(importsMessageType), meq(wrappedValidXML), any[LocalDateTime], any[UUID])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "pass utc date to connector" in new SetUp() {
      callSend() shouldBe Right(())

      verify(mockImportsConnector).send(any[ImportsMessageType], any[NodeSeq], meq(dateTime), any[UUID])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "call payload decorator passing incoming xml" in new SetUp() {
      callSend() shouldBe Right(())

      verify(mockPayloadDecorator).wrap(
        meq(TestXmlPayload),
        meq(apiSubscriptionFieldsResponse),
        any[Option[String]].asInstanceOf[Option[CorrelationIdHeader]],
        meq(importsMessageType.wrapperRootElementLabel),
        any[LocalDateTime],
        meq(CorrelationIdUuid))(any[ValidatedPayloadRequest[_]])
      verify(mockApiSubscriptionFieldsConnector).getSubscriptionFields(meq(expectedApiSubscriptionKeyV1))(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "return InternalServerError ErrorResponse when subscription fields call fails" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))
      callSend() shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)

      verifyNoMoreInteractions(mockPayloadDecorator)
      verifyNoMoreInteractions(mockImportsConnector)
    }

    "return InternalServerError ErrorResponse when subscription fields call succeeds but does not return a authenticatedEori value" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponseWithoutAuthenticatedEori))
      callSend() shouldBe Left(errorResponseMissingEori.XmlResult.withConversationId)

      verifyNoMoreInteractions(mockPayloadDecorator)
      verifyNoMoreInteractions(mockImportsConnector)
    }

    "return InternalServerError ErrorResponse when Mdg Import call fails" in new SetUp() {
      when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], any[LocalDateTime], any[UUID])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))
      callSend() shouldBe Left(ErrorInternalServerError.XmlResult.withConversationId)

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "return InternalServerError ErrorResponse when backend circuit breaker trips" in new SetUp() {
      when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], any[LocalDateTime], any[UUID])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(new CircuitBreakerOpenException(FiniteDuration(10, TimeUnit.SECONDS))))
      callSend() shouldBe Left(errorResponseServiceUnavailable.XmlResult)

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "return Forbidden ErrorResponse when backend returns 403" in new SetUp() {
      when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], any[LocalDateTime], any[UUID])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(new HttpException("Forbidden", FORBIDDEN)))
      callSend() shouldBe Left(ErrorPayloadForbidden.XmlResult.withConversationId)

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
      VerifyLogging.verifyImportsLoggerThrowable("warn", "Returning status=[403]. Forbidden")

    }

    "return InternalServerError ErrorResponse when backend returns 404 NOT_FOUND" in new SetUp() {
      when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], any[LocalDateTime], any[UUID])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(new HttpException("Not Found", NOT_FOUND)))
      callSend() shouldBe Left(ErrorInternalServerError.XmlResult.withConversationId)

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
      VerifyLogging.verifyImportsLoggerThrowable("warn", "Returning status=[500]. Not Found")
    }

    "return InternalServerError ErrorResponse when backend returns 502 BAD_GATEWAY" in new SetUp() {
      when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], any[LocalDateTime], any[UUID])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(new HttpException("BAD_GATEWAY", BAD_GATEWAY)))
      callSend() shouldBe Left(ErrorInternalServerError.XmlResult.withConversationId)

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
      VerifyLogging.verifyImportsLoggerThrowable("warn", "Returning status=[500]. BAD_GATEWAY")
    }
  }
}