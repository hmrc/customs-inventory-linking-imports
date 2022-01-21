/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.customs.inventorylinking.imports.model.VersionOne
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ApiVersionRequest, ConversationIdRequest, ExtractedHeaders, ValidatedHeadersRequest}
import util.CustomsMetricsTestData.EventStart
import util.TestData._
import util.{TestData, UnitSpec}

class HeaderValidatorActionSpec extends UnitSpec with TableDrivenPropertyChecks with MockitoSugar {

  trait SetUp {
    val loggerMock: ImportsLogger = mock[ImportsLogger]
    val validator = new HeaderValidator(loggerMock)
    val validatedRequest: ValidatedHeadersRequest[AnyContent] = mock[ValidatedHeadersRequest[AnyContent]]
    val request: ConversationIdRequest[AnyContent] = mock[ConversationIdRequest[AnyContent]]

    def validate(avr: ApiVersionRequest[_]): Either[ErrorResponse, ExtractedHeaders] = {
      validator.validateHeaders(avr)
    }
  }

  "HeaderValidatorAction" can {
    "in happy path, validation" should {
      "be successful for a valid request with accept header for V1" in new SetUp {
        validate(apiVersionRequest(ValidHeaders)) shouldBe Right(TestExtractedHeadersWithoutCorrelationId)
      }
      "be successful for content type XML with no space header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + (CONTENT_TYPE -> "application/xml;charset=utf-8"))) shouldBe Right(TestExtractedHeadersWithoutCorrelationId)
      }
      "be successful for missing X-Correlation-ID" in new SetUp {
        validate(apiVersionRequest(ValidHeaders - XCorrelationIdHeaderName)) shouldBe Right(TestExtractedHeadersWithoutCorrelationId)
      }
    }
    "in unhappy path, validation" should {
      "be unsuccessful for a valid request with missing accept header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders - XClientIdHeaderName)) shouldBe Left(ErrorInternalServerError)
      }
      "be unsuccessful for a valid request with missing content type header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders - CONTENT_TYPE)) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "be unsuccessful for a valid request with missing X-Client-ID header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders - XClientIdHeaderName)) shouldBe Left(ErrorInternalServerError)
      }
      "be successful for a valid request with missing X-Badge-Identifier header" in new SetUp {
        validate(apiVersionRequest(ValidHeadersNoCorrelationId - XBadgeIdentifierHeaderName)) shouldBe Right(TestExtractedHeadersWithoutCorrelationIdOrBadgeId)
      }
      "be successful for a valid request with missing X-Submitter-Identifier header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders - XSubmitterIdentifierHeaderName)) shouldBe Right(TestExtractedHeadersWithoutCorrelationIdOrSubmitterId)
      }
      "be successful for a valid request with missing X-Submitter-Identifier and X-Badge-Identifier headers" in new SetUp {
        validate(apiVersionRequest(ValidHeaders - XSubmitterIdentifierHeaderName - XBadgeIdentifierHeaderName)) shouldBe Right(TestExtractedHeadersWithoutCorrelationIdOrSubmitterIdOrBadgeId)
      }
      "be unsuccessful for a valid request with Invalid content type header JSON header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + InvalidContentTypeJsonHeader)) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "be unsuccessful for a valid request with Invalid content type XML without UTF-8 header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + (CONTENT_TYPE -> "application/xml"))) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "be unsuccessful for a valid request with Invalid X-Client-ID header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + InvalidXClientIdHeader)) shouldBe Left(ErrorInternalServerError)
      }
      "be unsuccessful for a valid request with Invalid X-Badge-Identifier header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + InvalidXBadgeIdentifier)) shouldBe Left(ErrorResponseBadgeIdentifierHeaderMissing)
      }
      "be unsuccessful for a valid request with an invalid X-Submitter-Identifier with non alphanumeric characters" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + InvalidXSubmitterIdentifierNonAlphanumeric)) shouldBe Left(ErrorResponseSubmitterIdentifierHeaderMissing)
      }
      "be unsuccessful for a valid request with an invalid X-Submitter-Identifier header greater than 17 characters" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + InvalidXSubmitterIdentifierLongerThan17)) shouldBe Left(ErrorResponseSubmitterIdentifierHeaderMissing)
      }
      "be unsuccessful for a valid request with an invalid X-Submitter-Identifier header that is empty" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + InvalidXSubmitterIdentifierEmpty)) shouldBe Left(ErrorResponseSubmitterIdentifierHeaderMissing)
      }
    }
  }

  private def apiVersionRequest(requestMap: Map[String, String]): ApiVersionRequest[_] =
    ApiVersionRequest(TestData.ValidConversationId, EventStart, VersionOne, FakeRequest().withHeaders(requestMap.toSeq: _*))

}
