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

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders.PayloadValidationAction
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ConversationIdRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{GoodsArrivalXmlValidationService, ValidateMovementXmlValidationService}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.{TestAuthorisedRequest, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.SAXException

class PayloadValidationActionSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  private implicit val forConversions = TestConversationIdRequest
  private val saxException = new SAXException("Boom!")
  private val expectedXmlSchemaErrorResult = ErrorResponse
    .errorBadRequest("Payload is not valid according to schema")
    .withErrors(ResponseContents("xml_validation_error", saxException.getMessage)).XmlResult.withConversationId
  val mockGoodsArrivalXmlValidationService = mock[GoodsArrivalXmlValidationService]
  val mockValidateMovementXmlValidationService = mock[ValidateMovementXmlValidationService]

  val mockImportsLogger: ImportsLogger = mock[ImportsLogger]
  val goodsArrivalPayloadValidationAction = new PayloadValidationAction(mockGoodsArrivalXmlValidationService, mockImportsLogger) {}
  val validateMovementPayloadValidationAction = new PayloadValidationAction(mockValidateMovementXmlValidationService, mockImportsLogger) {}

  val headersTable =
    Table(
      ("description", "xml service", "payload action"),
      ("Validate Movement", mockGoodsArrivalXmlValidationService, goodsArrivalPayloadValidationAction),
      ("Goods Arrival", mockValidateMovementXmlValidationService, validateMovementPayloadValidationAction)
    )

  "PayloadValidationAction" should  {
    forAll(headersTable) { (description, xmlService, payloadAction) =>

      s"return Right of ValidatedPayloadRequest when $description XML validation is OK" in {
        when(xmlService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.successful(()))

        val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(payloadAction.refine(TestAuthorisedRequest))

        actual shouldBe Right(TestCspValidatedPayloadRequest)
      }

      s"return Left of error Result when $description XML is not well formed" in {
        when(xmlService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(saxException))

        val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(payloadAction.refine(TestAuthorisedRequest))

        actual shouldBe Left(expectedXmlSchemaErrorResult)
      }

      s"return Left of error Result when $description XML validation fails" in {
        val errorMessage = "Request body does not contain a well-formed XML document."
        val errorNotWellFormed = ErrorResponse.errorBadRequest(errorMessage).XmlResult.withConversationId
        val authorisedRequestWithNonWellFormedXml = ConversationIdRequest(ValidConversationId, FakeRequest().withTextBody("<foo><foo>"))
          .toValidatedHeadersRequest(TestExtractedHeaders).toAuthorisedRequest

        val actual = await(payloadAction.refine(authorisedRequestWithNonWellFormedXml))

        actual shouldBe Left(errorNotWellFormed)
      }

      s"propagates downstream errors by returning Left of $description error Result" in {
        when(xmlService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(emulatedServiceFailure))

        val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(payloadAction.refine(TestAuthorisedRequest))

        actual shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
      }
    }
  }
}
