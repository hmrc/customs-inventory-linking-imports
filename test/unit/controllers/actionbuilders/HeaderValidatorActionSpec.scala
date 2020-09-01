/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.HeaderValidator
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ConversationIdRequest, ExtractedHeaders, ValidatedHeadersRequest}
import util.CustomsMetricsTestData.EventStart
import util.UnitSpec
import util.TestData
import util.TestData._

class HeaderValidatorActionSpec extends UnitSpec with TableDrivenPropertyChecks with MockitoSugar {

  trait SetUp {
    val loggerMock: ImportsLogger = mock[ImportsLogger]
    val validator = new HeaderValidator(loggerMock)
    val validatedRequest: ValidatedHeadersRequest[AnyContent] = mock[ValidatedHeadersRequest[AnyContent]]
    val request: ConversationIdRequest[AnyContent] = mock[ConversationIdRequest[AnyContent]]

    def validate(c: ConversationIdRequest[_]): Either[ErrorResponse, ExtractedHeaders] = {
      validator.validateHeaders(c)
    }
  }

  "HeaderValidatorAction" can {
    "in happy path, validation" should {
      "be successful for a valid request with accept header for V1" in new SetUp {
        validate(conversationIdRequest(ValidHeaders)) shouldBe Right(TestExtractedHeadersWithoutCorrelationId)
      }
      "be successful for content type XML with no space header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + (CONTENT_TYPE -> "application/xml;charset=utf-8"))) shouldBe Right(TestExtractedHeadersWithoutCorrelationId)
      }
      "be successful for missing X-Correlation-ID" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - XCorrelationIdHeaderName)) shouldBe Right(TestExtractedHeadersWithoutCorrelationId)
      }
    }
    "in unhappy path, validation" should {
      "be unsuccessful for a valid request with missing accept header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - XClientIdHeaderName)) shouldBe Left(ErrorInternalServerError)
      }
      "be unsuccessful for a valid request with missing content type header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - CONTENT_TYPE)) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "be unsuccessful for a valid request with missing X-Client-ID header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - XClientIdHeaderName)) shouldBe Left(ErrorInternalServerError)
      }
      "be successful for a valid request with missing X-Badge-Identifier header" in new SetUp {
        validate(conversationIdRequest(ValidHeadersNoCorrelationId - XBadgeIdentifierHeaderName)) shouldBe Right(TestExtractedHeadersWithoutCorrelationIdOrBadgeId)
      }
      "be successful for a valid request with missing X-Submitter-Identifier header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - XSubmitterIdentifierHeaderName)) shouldBe Right(TestExtractedHeadersWithoutCorrelationIdOrSubmitterId)
      }
      "be successful for a valid request with missing X-Submitter-Identifier and X-Badge-Identifier headers" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - XSubmitterIdentifierHeaderName - XBadgeIdentifierHeaderName)) shouldBe Right(TestExtractedHeadersWithoutCorrelationIdOrSubmitterIdOrBadgeId)
      }
      "be unsuccessful for a valid request with Invalid accept header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + InvalidAcceptHeader)) shouldBe Left(ErrorAcceptHeaderInvalid)
      }
      "be unsuccessful for a valid request with Invalid content type header JSON header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + InvalidContentTypeJsonHeader)) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "be unsuccessful for a valid request with Invalid content type XML without UTF-8 header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + (CONTENT_TYPE -> "application/xml"))) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "be unsuccessful for a valid request with Invalid X-Client-ID header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + InvalidXClientIdHeader)) shouldBe Left(ErrorInternalServerError)
      }
      "be unsuccessful for a valid request with Invalid X-Badge-Identifier header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + InvalidXBadgeIdentifier)) shouldBe Left(ErrorResponseBadgeIdentifierHeaderMissing)
      }
      "be unsuccessful for a valid request with an invalid X-Submitter-Identifier with non alphanumeric characters" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + InvalidXSubmitterIdentifierNonAlphanumeric)) shouldBe Left(ErrorResponseSubmitterIdentifierHeaderMissing)
      }
      "be unsuccessful for a valid request with an invalid X-Submitter-Identifier header greater than 17 characters" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + InvalidXSubmitterIdentifierLongerThan17)) shouldBe Left(ErrorResponseSubmitterIdentifierHeaderMissing)
      }
      "be unsuccessful for a valid request with an invalid X-Submitter-Identifier header that is empty" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + InvalidXSubmitterIdentifierEmpty)) shouldBe Left(ErrorResponseSubmitterIdentifierHeaderMissing)
      }
    }
  }

  private def conversationIdRequest(requestMap: Map[String, String]): ConversationIdRequest[_] =
    ConversationIdRequest(TestData.ValidConversationId, EventStart, FakeRequest().withHeaders(requestMap.toSeq: _*))

}
