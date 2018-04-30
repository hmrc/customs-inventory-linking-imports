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

import java.util.concurrent.TimeUnit.SECONDS

import akka.stream.Materializer
import akka.util.Timeout
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, UNAUTHORIZED}
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, header}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, InsufficientEnrolments}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.UnauthorizedCode
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders.AuthAction
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, RequestData, ValidateMovement, ValidatedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData._
import util.TestData._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Node, NodeSeq, Utility, XML}

class AuthActionSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  private val blockReturningOk = (_: ValidatedRequest[_]) => Future.successful(Ok)
  private val unauthorisedResponse =
    ErrorResponse(Status.UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")
  private val UuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
  private implicit val mockMaterialiser = mock[Materializer]
  private implicit val timeout = Timeout(5L, SECONDS)
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

  private val requestData = RequestData(
    XBadgeIdentifierHeaderValueAsString,
    ConversationId.toString,
    CorrelationId.toString,
    dateTime = DateTime.now(),
    requestedApiVersion = "1.0",
    TestXClientId
  )

  trait SetUp {
    private lazy val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockLogger = mock[ImportsLogger]
    val authActionBuilder = new AuthAction(mockAuthConnector, mockLogger)
    val request = FakeRequest().withXmlBody(NodeSeq.Empty)
    val validationRequest = ValidatedRequest(requestData, request)

    def authoriseCsp(predicate: Predicate): Unit = {
      when(mockAuthConnector.authorise(ameq(predicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(())
    }

    def unAuthoriseCsp(predicate: Predicate, authException: AuthorisationException = new InsufficientEnrolments): Unit = {
      when(mockAuthConnector.authorise(ameq(predicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(authException))
    }

    def authError(predicate: Predicate, authException: Exception = emulatedServiceFailure): Unit = {
      when(mockAuthConnector.authorise(ameq(predicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(authException))
    }
  }

  val headersTable =
    Table(
      ("description", "Type", "Auth Predicate", "Expected response"),
      ("Validate Movement", ValidateMovement, ValidateMovementAuthPredicate, Ok),
      ("Goods Arrival", GoodsArrival, GoodsArrivalAuthPredicate, Ok)
    )

  "AuthAction" should  {
    forAll(headersTable) { (description, msgType, predicate, okResult) =>
      s"invoke action block for $description when auth-client returns authorised" in new SetUp() {
        authoriseCsp(predicate)

        val actualResult: Result = await(authActionBuilder.authAction(msgType).invokeBlock(validationRequest, blockReturningOk))

        actualResult shouldBe okResult
      }

      s"return 401 response for $description when auth-client returns un-authorised" in new SetUp() {
        unAuthoriseCsp(predicate)

        val actualResult: Result = await(authActionBuilder.authAction(msgType).invokeBlock(validationRequest, blockReturningOk))

        status(actualResult) shouldBe UNAUTHORIZED
        stringToXml(contentAsString(actualResult)) shouldBe stringToXml(unAuthorisedError)
        header(XConversationIdHeaderName, actualResult).get should fullyMatch regex UuidRegex
      }

      s"return 500 response for $description when auth-client returns un-authorised" in new SetUp() {
        authError(predicate)

        val actualResult: Result = await(authActionBuilder.authAction(msgType).invokeBlock(validationRequest, blockReturningOk))

        status(actualResult) shouldBe INTERNAL_SERVER_ERROR
        stringToXml(contentAsString(actualResult)) shouldBe stringToXml(internalServerError)
        header(XConversationIdHeaderName, actualResult).get should fullyMatch regex UuidRegex
      }
    }
  }

  private def stringToXml(str: String): Node = {
    Utility.trim(XML.loadString(str))
  }

}
