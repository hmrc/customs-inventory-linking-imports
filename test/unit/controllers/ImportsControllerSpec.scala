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

import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

import akka.stream.Materializer
import akka.util.Timeout
import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.{reset, verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.xml.sax.SAXException
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.http.MimeTypes
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{UNAUTHORIZED, contentAsString, header}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthorisationException, InsufficientEnrolments}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.{GoodsArrivalController, ValidateMovementController}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.DeclarationsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, RequestDataWrapper, ValidateMovement}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{ImportsConfigService, MessageSender}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData
import util.TestData._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Node, Utility, XML}

class ImportsControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with TableDrivenPropertyChecks with BeforeAndAfterEach {

  private val mockAuthConnector: MicroserviceAuthConnector = mock[MicroserviceAuthConnector]
  private val serviceConfigProvider: ServiceConfigProvider = mock[ServiceConfigProvider]
  private val badgeIdentifier = "BADGE1"
  private val messageSender: MessageSender = mock[MessageSender]
  private val configuration = mock[ImportsConfigService]
  private implicit val mockMaterialiser = mock[Materializer]
  private implicit val timeout = Timeout(5L, SECONDS)
  private val request: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(outgoingBody).
    withHeaders(
      ACCEPT -> AcceptHeaderValue,
      CONTENT_TYPE -> (MimeTypes.XML + "; charset=utf-8"),
      XClientIdHeaderName -> ApiSubscriptionFieldsTestData.TestXClientId,
      XBadgeIdentifierHeaderName -> badgeIdentifier)

  private val logger = mock[DeclarationsLogger]
  private val validateMovementController: ValidateMovementController = new ValidateMovementController(configuration, mockAuthConnector, messageSender, logger)
  private val goodsArrivalController: GoodsArrivalController = new GoodsArrivalController(configuration, mockAuthConnector, messageSender, logger)
  private val UuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
  private val errorResultUnauthorised = ErrorResponse(UNAUTHORIZED, errorCode = "UNAUTHORIZED",
    message = "Unauthorised request").XmlResult.withHeaders("X-Conversation-ID" -> conversationId.toString)

  private implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]
  private implicit val rdWrapper: RequestDataWrapper = RequestDataWrapper(request, headerCarrier)

  private val defaultUuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

  private val internalServerError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>INTERNAL_SERVER_ERROR</code>
      |  <message>Internal server error</message>
      |</errorResponse>
    """.stripMargin

  private val unAuthorisedError =
    """<errorResponse>
      |    <code>UNAUTHORIZED</code>
      |    <message>Unauthorised request</message>
      |</errorResponse>""".stripMargin

  override protected def beforeEach() {
    reset(serviceConfigProvider, messageSender)
  }


  private val controllers = Table(("Message Type Name", "Imports Message Type", "Auth Predicate", "controller post"),
    ("Goods Arrival", GoodsArrival, GoodsArrivalAuthPredicate, goodsArrivalController.post()),
    ("Validate Movement", ValidateMovement, ValidateMovementAuthPredicate, validateMovementController.post())
  )

  forAll(controllers) { case (messageTypeName, importsMessageType,  authPredicate, controller) =>

    "POST valid declaration" when {
      "message is sent successfully" should {
        s"return 202 ACCEPTED for $messageTypeName" in {

          authoriseCsp(authPredicate)
          when(messageSender.validateAndSend(ameq(importsMessageType))(any[RequestDataWrapper])).
            thenReturn(Future.successful(HttpResponse(ACCEPTED)))

          val result = await(controller.apply(request))

          status(result) shouldBe ACCEPTED
        }

        s"return X-Conversation-Id header for $messageTypeName" in {

          authoriseCsp(authPredicate)
          when(messageSender.validateAndSend(importsMessageType)(rdWrapper)).
            thenReturn(Future.successful(HttpResponse(ACCEPTED)))

          val result = await(controller.apply(request))

          result.header.headers.getOrElse("X-Conversation-ID", defaultUuid) shouldNot be(defaultUuid)
        }
      }


      "called by unauthorised CSP" should {
        s"return result 401 UNAUTHORISED for $messageTypeName" in {
          unauthoriseCsp(authPredicate)
          val result: Result = await(controller.apply(request))

          status(result) shouldBe UNAUTHORIZED
          stringToXml(contentAsString(result)) shouldBe stringToXml(unAuthorisedError)
          header(XConversationIdHeaderName, result).get should fullyMatch regex UuidRegex

          verifyZeroInteractions(messageSender)
        }
      }

      s"message fails due to backend service error for $messageTypeName" should {
        "return 500 Internal Server Error" in {

          authoriseCsp(authPredicate)
          when(messageSender.validateAndSend(importsMessageType)(rdWrapper)).
            thenReturn(Future.failed(emulatedServiceFailure))

          val result = await(controller.apply(request))

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

        s"return X-Conversation-Id header for $messageTypeName" in {

          authoriseCsp(authPredicate)
          when(messageSender.validateAndSend(importsMessageType)(rdWrapper)).
            thenReturn(Future.failed(emulatedServiceFailure))

          val result = await(controller.apply(request))

          status(result) shouldBe INTERNAL_SERVER_ERROR
          stringToXml(contentAsString(result)) shouldBe stringToXml(internalServerError)
          header(XConversationIdHeaderName, result).get should fullyMatch regex UuidRegex

        }
      }
    }

    s"POST invalid declaration for $messageTypeName" should {
      "return bad request" in {

        authoriseCsp(authPredicate)
        when(messageSender.validateAndSend(ameq(importsMessageType))(any[RequestDataWrapper])).
          thenReturn(Future.failed(new SAXException()))

        val result = await(controller.apply(request))

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  private def stringToXml(str: String): Node = {
    Utility.trim(XML.loadString(str))
  }

  private def authoriseCsp(predicate: Predicate): Unit = {
    when(mockAuthConnector.authorise(ameq(predicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(())
  }

  private def unauthoriseCsp(predicate: Predicate, authException: AuthorisationException = new InsufficientEnrolments): Unit = {
    when(mockAuthConnector.authorise(ameq(predicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(authException))
  }
}
