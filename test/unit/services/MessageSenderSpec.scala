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

package unit.services

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{atLeastOnce, verify, verifyZeroInteractions, when}
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsXml, Result}
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorInternalServerError
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ApiSubscriptionFieldsConnector, ImportsConnector}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.services.{ImportsConfigService, _}
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData._
import util.TestData.{TestCspValidatedPayloadRequest, TestXmlPayload, _}

import scala.concurrent.Future
import scala.xml.NodeSeq

class MessageSenderSpec extends UnitSpec with Matchers with MockitoSugar with TableDrivenPropertyChecks {

  private val dateTime = new DateTime()
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val expectedApiSubscriptionKey = ApiSubscriptionKey(clientId, "customs%2Finventory-linking-imports", VersionOne)
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest
  private val wrappedValidXML = <wrapped></wrapped>
  private val errorResponseServiceUnavailable = errorInternalServerError("This service is currently unavailable")

  trait SetUp {
    protected val importsMessageType: ImportsMessageType = new GoodsArrival()
    protected val mockLogger: ImportsLogger = mock[ImportsLogger]
    protected val mockImportsConnector: ImportsConnector = mock[ImportsConnector]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockPayloadDecorator: PayloadDecorator = mock[PayloadDecorator]
    protected val mockDateTimeProvider: DateTimeService = mock[DateTimeService]
    protected val mockHttpResponse: HttpResponse = mock[HttpResponse]
    protected val mockImportsConfigService: ImportsConfigService = mock[ImportsConfigService]

    protected lazy val service: MessageSender = new MessageSender(mockApiSubscriptionFieldsConnector, mockPayloadDecorator, mockImportsConnector,
      mockDateTimeProvider, stubUniqueIdsService, mockLogger, mockImportsConfigService)

    protected def callSend(vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest, hc: HeaderCarrier = headerCarrier): Either[Result, Unit] = {
      await(service.send(importsMessageType)(vpr, hc))
    }

    when(mockPayloadDecorator.wrap(
      meq(TestXmlPayload),
      meq(apiSubscriptionFieldsResponse),
      any[Option[String]].asInstanceOf[Option[CorrelationIdHeader]],
      meq(importsMessageType.wrapperRootElementLabel),
      any[DateTime],
      meq(CorrelationIdUuid))(any[ValidatedPayloadRequest[_]])
    ).thenReturn(wrappedValidXML)

    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], meq(dateTime), any[UUID])(any[ValidatedPayloadRequest[_]])).thenReturn(mockHttpResponse)
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
    when(mockImportsConfigService.importsConfig).thenReturn(ImportsConfig(Seq(), "https://random.url"))
  }

  "MessageSender" should {

    "send transformed xml to connector" in new SetUp() {
      callSend() shouldBe Right(())

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
      verify(mockImportsConnector).send(meq(importsMessageType), meq(wrappedValidXML), any[DateTime], any[UUID])(any[ValidatedPayloadRequest[_]])
    }

    "pass utc date to connector" in new SetUp() {
      callSend() shouldBe Right(())

      verify(mockImportsConnector).send(any[ImportsMessageType], any[NodeSeq], meq(dateTime), any[UUID])(any[ValidatedPayloadRequest[_]])
      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "call payload decorator passing incoming xml" in new SetUp() {
      callSend() shouldBe Right(())

      verify(mockPayloadDecorator).wrap(
        meq(TestXmlPayload),
        meq(apiSubscriptionFieldsResponse),
        any[Option[String]].asInstanceOf[Option[CorrelationIdHeader]],
        meq(importsMessageType.wrapperRootElementLabel),
        any[DateTime],
        meq(CorrelationIdUuid))(any[ValidatedPayloadRequest[_]])
      verify(mockApiSubscriptionFieldsConnector).getSubscriptionFields(meq(expectedApiSubscriptionKey))(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "return InternalServerError ErrorResponse when subscription fields call fails" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))
      callSend() shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)

      verifyZeroInteractions(mockPayloadDecorator)
      verifyZeroInteractions(mockImportsConnector)
    }

    "return InternalServerError ErrorResponse when Mdg Import call fails" in new SetUp() {
      when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], any[DateTime], any[UUID])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))
      callSend() shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "return InternalServerError ErrorResponse when backend circuit breaker trips" in new SetUp() {
      when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], any[DateTime], any[UUID])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(new UnhealthyServiceException("wco-declaration")))
      callSend() shouldBe Left(errorResponseServiceUnavailable.XmlResult)

      verify(mockApiSubscriptionFieldsConnector, atLeastOnce()).getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

  }
}