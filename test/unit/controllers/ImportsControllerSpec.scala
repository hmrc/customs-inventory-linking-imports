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

package unit.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders._
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.{Common, GoodsArrivalController, HeaderValidator}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsMessageType}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{GoodsArrivalXmlValidationService, MessageSender}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.AuthConnectorStubbing
import util.TestData._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class ImportsControllerSpec extends UnitSpec
  with Matchers with MockitoSugar with BeforeAndAfterEach {

  trait SetUp extends AuthConnectorStubbing {

    override val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockImportsLogger: ImportsLogger = mock[ImportsLogger]
    protected val mockMessageSender: MessageSender = mock[MessageSender]
    protected val mockResult: Result = mock[Result]
    protected val mockGoodsArrivalXmlValidationService: GoodsArrivalXmlValidationService = mock[GoodsArrivalXmlValidationService]
    protected val mockGoodsArrival: GoodsArrival = mock[GoodsArrival]
    protected val mockCommon: Common = mock[Common]

    protected val stubConversationIdAction: ConversationIdAction = new ConversationIdAction(stubUniqueIdsService, mockImportsLogger)
    protected val stubGoodsArrivalAuthAction = new GoodsArrivalAuthAction(mockAuthConnector, mockGoodsArrival, mockImportsLogger)
    protected val stubValidateAndExtractHeadersAction: ValidateAndExtractHeadersAction = new ValidateAndExtractHeadersAction(new HeaderValidator(mockImportsLogger), mockImportsLogger)
    protected val stubGoodsArrivalPayloadValidationAction: GoodsArrivalPayloadValidationAction = new GoodsArrivalPayloadValidationAction(mockGoodsArrivalXmlValidationService, mockImportsLogger)

    protected val enrolment: Enrolment = Enrolment("write:customs-il-imports-arrival-notifications")

    protected val controller: GoodsArrivalController = new GoodsArrivalController(mockCommon, mockGoodsArrival, stubGoodsArrivalAuthAction, stubGoodsArrivalPayloadValidationAction, stubValidateAndExtractHeadersAction)

    protected def awaitSubmit(request: Request[AnyContent]): Result = {
      await(controller.post().apply(request))
    }

    protected def submit(request: Request[AnyContent]): Future[Result] = {
      controller.post().apply(request)
    }

    when(mockCommon.conversationIdAction).thenReturn(stubConversationIdAction)
    when(mockCommon.messageSender).thenReturn(mockMessageSender)

    when(mockGoodsArrivalXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext])).thenReturn(Future.successful(()))
    when(mockMessageSender.send(any[GoodsArrival])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(Right(())))
    when(mockGoodsArrival.enrolment).thenReturn(Enrolment("write:customs-il-imports-arrival-notifications"))
  }

  private val errorResultUnauthorised = ErrorResponse(UNAUTHORIZED, errorCode = "UNAUTHORIZED",
    message = "Unauthorised request").XmlResult.withHeaders(XConversationIdHeader)

  "InventoryLinkingImportController" should {
    "process CSP request when call is authorised for CSP" in new SetUp {
      authoriseCsp(enrolment)

      awaitSubmit(ValidRequest)

      verifyCspAuthorisationCalled(enrolment, numberOfTimes = 1)
    }

    "respond with status 202 and conversationId in header for a processed valid CSP request" in new SetUp {
      authoriseCsp(enrolment)

      val result: Future[Result] = submit(ValidRequest)

      status(result) shouldBe ACCEPTED
      header(XConversationIdHeaderName, result) shouldBe Some(ConversationIdValue)
    }

    "return result 401 UNAUTHORISED and conversationId in header when call is unauthorised" in new SetUp {
      unauthoriseCsp(enrolment)

      val result: Future[Result] = submit(ValidRequest)

      await(result) shouldBe errorResultUnauthorised
      header(XConversationIdHeaderName, result) shouldBe Some(ConversationIdValue)
      verifyZeroInteractions(mockMessageSender)
      verifyZeroInteractions(mockGoodsArrivalXmlValidationService)
    }

    "return the error response returned from the Communication service" in new SetUp {
      when(mockMessageSender.send(any[ImportsMessageType])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(mockResult)))
      authoriseCsp(enrolment)

      awaitSubmit(ValidRequest) shouldBe mockResult
    }
  }

}
