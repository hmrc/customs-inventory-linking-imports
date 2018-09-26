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

package unit.controllers.actionbuilders

import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.HeaderValidator
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ConversationIdRequest, ExtractedHeaders, ValidatedHeadersRequest}
import uk.gov.hmrc.play.test.UnitSpec
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
        validate(conversationIdRequest(ValidHeaders)) shouldBe Right(TestExtractedHeaders)
      }
      "be successful for content type XML with no space header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + (CONTENT_TYPE -> "application/xml;charset=utf-8"))) shouldBe Right(TestExtractedHeaders)
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
      "be unsuccessful for a valid request with missing X-Badge-Identifier header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - XBadgeIdentifierHeaderName)) shouldBe Left(ErrorResponseBadgeIdentifierHeaderMissing)
      }
      "be unsuccessful for a valid request with missing X-Correlation-ID header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - XCorrelationIdHeaderName)) shouldBe Left(ErrorResponseCorrelationIdHeaderMissing)
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
      "be unsuccessful for a valid request with Invalid X-Correlation-ID header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + InvalidXCorrelationId)) shouldBe Left(ErrorResponseCorrelationIdHeaderMissing)
      }
    }
  }

  private def conversationIdRequest(requestMap: Map[String, String]): ConversationIdRequest[_] =
    ConversationIdRequest(TestData.ValidConversationId, FakeRequest().withHeaders(requestMap.toSeq: _*))

}
