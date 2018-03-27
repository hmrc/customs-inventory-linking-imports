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
import play.api.mvc.{AnyContent, Headers, Request, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.{HeaderValidator, ValidateAndExtractHeadersAction}
import uk.gov.hmrc.customs.inventorylinking.imports.model.ValidatedRequest
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.ValidHeaders

class ValidateAndExtractHeadersActionTest extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  // TODO use less mocking and use ActionBuilder.invokeBlock()

  trait SetUp {
    val request = mock[Request[AnyContent]]
    val loggerMock = mock[CdsLogger]
    val headerValidatorMock = mock[HeaderValidator]
    val actionBuilderValidator = new ValidateAndExtractHeadersAction(headerValidatorMock, loggerMock)
  }

  val headersTable =
    Table(
      ("description", "ValidationResult", "Expected response"),
      ("Valid Headers", Right(()), Right()),
      ("Missing content type header", Left(mock[ErrorResponse]), Left())
    )

  "HeaderValidatorAction" should  {
    forAll(headersTable) { (description, validationReslt, response) =>
      s"$description" in new SetUp() {
        when(request.headers).thenReturn(new Headers(ValidHeaders.toSeq))
        when(headerValidatorMock.validateHeaders(request, loggerMock)).thenReturn(validationReslt)

        val acutal: Either[Result, ValidatedRequest[AnyContent]] = await(actionBuilderValidator.refine(request))

        acutal shouldBe an[response.type]

      }
    }
  }
}
