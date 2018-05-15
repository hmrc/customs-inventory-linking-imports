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
import org.mockito.Mockito.{verify, verifyZeroInteractions, when}
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsXml, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ApiSubscriptionFieldsConnector, ImportsConnector}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.services._
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

  trait SetUp {
    protected val importsMessageType: ImportsMessageType = new GoodsArrival()
    protected val mockLogger: ImportsLogger = mock[ImportsLogger]
    protected val mockImportsConnector: ImportsConnector = mock[ImportsConnector]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockPayloadDecorator: PayloadDecorator = mock[PayloadDecorator]
    protected val mockDateTimeProvider: DateTimeService = mock[DateTimeService]
    protected val mockHttpResponse: HttpResponse = mock[HttpResponse]

    protected lazy val service: MessageSender = new MessageSender(mockApiSubscriptionFieldsConnector, mockPayloadDecorator, mockImportsConnector,
      mockDateTimeProvider, stubUniqueIdsService, mockLogger)

    protected def send(vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest, hc: HeaderCarrier = headerCarrier): Either[Result, Unit] = {
      await(service.send(importsMessageType)(vpr, hc))
    }

    when(mockPayloadDecorator.wrap(meq(TestXmlPayload), meq[String](fieldsIdString).asInstanceOf[FieldsId], meq[UUID](CorrelationIdUuid).asInstanceOf[CorrelationId], meq(importsMessageType.wrapperRootElementLabel), any[DateTime])(any[ValidatedPayloadRequest[_]])).thenReturn(wrappedValidXML)
    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], meq(dateTime), any[UUID])(any[ValidatedPayloadRequest[_]])).thenReturn(mockHttpResponse)
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
  }

  "MessageSender" should {

    "send transformed xml to connector" in new SetUp() {

      val result: Either[Result, Unit] = send()

      result shouldBe Right(())
      verify(mockImportsConnector).send(meq(importsMessageType), meq(wrappedValidXML), any[DateTime], any[UUID])(any[ValidatedPayloadRequest[_]])
    }
  }


  "get utc date time and pass to connector" in new SetUp() {

    val result: Either[Result, Unit] = send()

    result shouldBe Right(())
    verify(mockImportsConnector).send(any[ImportsMessageType], any[NodeSeq], meq(dateTime), any[UUID])(any[ValidatedPayloadRequest[_]])
  }

  "call payload decorator passing incoming xml" in new SetUp() {

    val result: Either[Result, Unit] = send()

    result shouldBe Right(())
    verify(mockPayloadDecorator).wrap(meq(TestXmlPayload), meq[String](fieldsIdString).asInstanceOf[FieldsId], meq[UUID](CorrelationIdUuid).asInstanceOf[CorrelationId], meq(importsMessageType.wrapperRootElementLabel), any[DateTime])(any[ValidatedPayloadRequest[_]])
    verify(mockApiSubscriptionFieldsConnector).getSubscriptionFields(meq(expectedApiSubscriptionKey))(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
  }

  "return Left of error Result when subscription fields call fails" in new SetUp() {
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))

    val result: Either[Result, Unit] = send()

    result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    verifyZeroInteractions(mockPayloadDecorator)
    verifyZeroInteractions(mockImportsConnector)
  }

  "return Left of error Result when backend call fails" in new SetUp() {
    when(mockImportsConnector.send(any[ImportsMessageType], any[NodeSeq], any[DateTime], any[UUID])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))

    val result: Either[Result, Unit] = send()

    result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
  }

}
