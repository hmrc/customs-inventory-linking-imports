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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorContentTypeHeaderInvalid
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.HeaderValidator
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders.ValidateAndExtractHeadersAction
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ConversationIdRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._

class ValidateAndExtractHeadersActionSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  trait SetUp {
    val mockLogger = mock[ImportsLogger]
    val mockHeaderValidator = mock[HeaderValidator]
    val actionBuilderValidator = new ValidateAndExtractHeadersAction(mockHeaderValidator, mockLogger)
  }

  val headersTable =
    Table(
      ("description", "result of validation", "expected response"),
      ("Valid Headers", Right(TestExtractedHeaders), Right(TestValidatedHeadersRequest)),
      ("Invalid header", Left(ErrorContentTypeHeaderInvalid),
        Left(ErrorContentTypeHeaderInvalid.XmlResult.withHeaders(XConversationIdHeaderName -> ConversationIdValue)))
    )

  "HeaderValidatorAction" should  {
    forAll(headersTable) { (description, validationResult, expectedResult) =>
      s"$description" in new SetUp() {
        val conversationIdRequest = TestConversationIdRequest
        when(mockHeaderValidator.validateHeaders(any[ConversationIdRequest[AnyContent]])).thenReturn(validationResult)

        val actualResult: Either[Result, ValidatedHeadersRequest[_]] = await(actionBuilderValidator.refine(conversationIdRequest))

        actualResult shouldBe expectedResult
      }
    }
  }
}
