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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.xml.sax.SAXException
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, header}
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.PayloadValidationAction
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, RequestData, ValidateMovement, ValidatedRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{GoodsArrivalXmlValidationService, ValidateMovementXmlValidationService}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData._
import util.TestData
import util.TestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.xml._

class PayloadValidationActionSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  private val blockReturningOk = (_: ValidatedRequest[_]) => Future.successful(Ok)
  private val UuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
  private implicit val mockMaterialiser = mock[Materializer]
  private implicit val timeout = Timeout(5L, SECONDS)
  private val badRequestError =
    """|<?xml version="1.0" encoding="UTF-8"?>
       |<errorResponse>
       |   <code>BAD_REQUEST</code>
       |   <message>Bad Request</message>
       |   <errors>
       |      <error>
       |         <code>xml_validation_error</code>
       |         <message />
       |      </error>
       |   </errors>
       |</errorResponse>
    """.stripMargin

  private val internalServerError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>INTERNAL_SERVER_ERROR</code>
      |  <message>Internal server error</message>
      |</errorResponse>
    """.stripMargin

  private val requestData = RequestData(
    XBadgeIdentifierHeaderValueAsString,
    ConversationId.toString,
    CorrelationId.toString,
    dateTime = DateTime.now(),
    body = NodeSeq.Empty,
    requestedApiVersion = "1.0",
    TestXClientId
  )

  trait SetUp {
    val mockLogger = mock[ImportsLogger]
    val mockGoodsArrivalXmlValidationService = mock[GoodsArrivalXmlValidationService]
    val mockValidateMovementXmlValidationService = mock[ValidateMovementXmlValidationService]
    val payloadValidationAction = new PayloadValidationAction(mockGoodsArrivalXmlValidationService, mockValidateMovementXmlValidationService, mockLogger)
    val request = FakeRequest().withXmlBody(NodeSeq.Empty)
    implicit val validationRequest = ValidatedRequest[AnyContent](requestData, request)

  }

  val headersTable =
    Table(
      ("description", "Type", "Auth Predicate", "Expected response"),
      ("Validate Movement", ValidateMovement, ValidateMovementAuthPredicate, Ok),
      ("Goods Arrival", GoodsArrival, GoodsArrivalAuthPredicate, Ok)
    )

  "PayloadValidationAction" should  {
    forAll(headersTable) { (description, msgType, predicate, okResult) =>
      s"invoke action block for $description when valid xml is sent returns OK" in new SetUp() {
        when(mockGoodsArrivalXmlValidationService.validate(NodeSeq.Empty)).thenReturn(Future.successful(()))
        when(mockValidateMovementXmlValidationService.validate(NodeSeq.Empty)).thenReturn(Future.successful(()))

        val actualResult: Result = await(payloadValidationAction.validatePayload(msgType).invokeBlock(validationRequest, blockReturningOk))

        actualResult shouldBe okResult
      }

      s"return $BAD_REQUEST response for $description when invalid xml is sent" in new SetUp() {

        when(mockGoodsArrivalXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext], any[ValidatedRequest[AnyContent]])).thenReturn(Future.failed(new SAXException()))
        when(mockValidateMovementXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext], any[ValidatedRequest[AnyContent]])).thenReturn(Future.failed(new SAXException()))

        val actualResult: Result = await(payloadValidationAction.validatePayload(msgType).invokeBlock(validationRequest, blockReturningOk))

        status(actualResult) shouldBe BAD_REQUEST
        stringToXml(contentAsString(actualResult)) shouldBe stringToXml(badRequestError)
        header(XConversationIdHeaderName, actualResult).get should fullyMatch regex UuidRegex
      }

      s"return $INTERNAL_SERVER_ERROR response for $description when invalid xml is sent" in new SetUp() {

        when(mockGoodsArrivalXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext], any[ValidatedRequest[AnyContent]])).thenReturn(Future.failed(TestData.emulatedServiceFailure))
        when(mockValidateMovementXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext], any[ValidatedRequest[AnyContent]])).thenReturn(Future.failed(TestData.emulatedServiceFailure))

        val actualResult: Result = await(payloadValidationAction.validatePayload(msgType).invokeBlock(validationRequest, blockReturningOk))

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
