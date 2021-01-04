/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.controllers.actionbuilders

import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders.PayloadValidationAction
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ConversationIdRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{GoodsArrivalXmlValidationService, ValidateMovementXmlValidationService}
import util.CustomsMetricsTestData.EventStart
import util.UnitSpec
import util.TestData.{TestAuthorisedRequest, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.SAXException

class PayloadValidationActionSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  trait SetUp{
    implicit val forConversions: ConversationIdRequest[AnyContentAsXml] = TestConversationIdRequestV1
    val saxException = new SAXException("Boom!")

    val expectedXmlSchemaErrorResult: Result = ErrorResponse
      .errorBadRequest("Payload is not valid according to schema")
      .withErrors(ResponseContents("xml_validation_error", saxException.getMessage)).XmlResult.withConversationId

    val errorNotWellFormedResult: Result = ErrorResponse
      .errorBadRequest("Request body does not contain a well-formed XML document.")
      .XmlResult.withConversationId

    val mockGoodsArrivalXmlValidationService: GoodsArrivalXmlValidationService = mock[GoodsArrivalXmlValidationService]
    val mockValidateMovementXmlValidationService: ValidateMovementXmlValidationService = mock[ValidateMovementXmlValidationService]

    val mockImportsLogger: ImportsLogger = mock[ImportsLogger]
    val goodsArrivalPayloadValidationAction: PayloadValidationAction = new PayloadValidationAction(mockGoodsArrivalXmlValidationService, mockImportsLogger) {}
    val validateMovementPayloadValidationAction: PayloadValidationAction = new PayloadValidationAction(mockValidateMovementXmlValidationService, mockImportsLogger) {}
  }

  "PayloadValidationAction for Validate Movement" should {
      "return a ValidatedPayloadRequest when XML validation is OK" in new SetUp {
        when(mockValidateMovementXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.successful(()))

        private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(validateMovementPayloadValidationAction.refine(TestAuthorisedRequest))

        actual shouldBe Right(TestCspValidatedPayloadRequest)
      }

      "return 400 error response when XML validation fails" in new SetUp {
        when(mockValidateMovementXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(saxException))

        private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(validateMovementPayloadValidationAction.refine(TestAuthorisedRequest))

        actual shouldBe Left(expectedXmlSchemaErrorResult)
      }

    "return 400 error response when XML is not well formed" in new SetUp {
      private val authorisedRequestWithNonWellFormedXml = ConversationIdRequest(ValidConversationId, EventStart, FakeRequest().withTextBody("<foo><foo>"))
        .toValidatedHeadersRequest(TestExtractedHeadersV1).toAuthorisedRequest

      private val actual = await(validateMovementPayloadValidationAction.refine(authorisedRequestWithNonWellFormedXml))

      actual shouldBe Left(errorNotWellFormedResult)
    }


    "propagates downstream errors by returning a 500 error response" in new SetUp {
      when(mockValidateMovementXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(emulatedServiceFailure))

      val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(validateMovementPayloadValidationAction.refine(TestAuthorisedRequest))

      actual shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  "PayloadValidationAction for Goods Arrival" should {
    "return a ValidatedPayloadRequest when XML validation is OK" in new SetUp {
      when(mockGoodsArrivalXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.successful(()))

      private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(goodsArrivalPayloadValidationAction.refine(TestAuthorisedRequest))

      actual shouldBe Right(TestCspValidatedPayloadRequest)
    }

    "return 400 error response when XML validation fails" in new SetUp {
      when(mockGoodsArrivalXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(saxException))

      private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(goodsArrivalPayloadValidationAction.refine(TestAuthorisedRequest))

      actual shouldBe Left(expectedXmlSchemaErrorResult)
    }

    "return 400 error response when XML is not well formed" in new SetUp {
      private val authorisedRequestWithNonWellFormedXml = ConversationIdRequest(ValidConversationId, EventStart, FakeRequest().withTextBody("<foo><foo>"))
        .toValidatedHeadersRequest(TestExtractedHeadersV1).toAuthorisedRequest

      private val actual = await(goodsArrivalPayloadValidationAction.refine(authorisedRequestWithNonWellFormedXml))

      actual shouldBe Left(errorNotWellFormedResult)
    }


    "propagates downstream errors by returning a 500 error response" in new SetUp {
      when(mockGoodsArrivalXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(emulatedServiceFailure))

      val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(goodsArrivalPayloadValidationAction.refine(TestAuthorisedRequest))

      actual shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

}
