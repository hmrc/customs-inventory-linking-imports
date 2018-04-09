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

import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorContentTypeHeaderInvalid
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.HeaderValidator
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders.ValidateAndExtractHeadersAction
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants._
import uk.gov.hmrc.customs.inventorylinking.imports.model.ValidatedRequest
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData._
import util.TestData._
import util.XMLTestData

import scala.concurrent.Future

class ValidateAndExtractHeadersActionSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  val blockReturningOk = (_: ValidatedRequest[_]) => Future.successful(Ok)

  trait SetUp {
    val mockLogger = mock[ImportsLogger]
    val mockHeaderValidator = mock[HeaderValidator]
    val actionBuilderValidator = new ValidateAndExtractHeadersAction(mockHeaderValidator, mockLogger)
  }

  val headersTable =
    Table(
      ("description", "ValidationResult", "Expected response"),
      ("Valid Headers", Right(TestExtractedHeaders), Ok),
      ("Invalid header", Left(ErrorContentTypeHeaderInvalid), ErrorContentTypeHeaderInvalid.XmlResult)
    )

  "HeaderValidatorAction" should  {
    forAll(headersTable) { (description, validationResult, expectedResult) =>
      s"$description" in new SetUp() {
        val request = FakeRequest().withXmlBody(XMLTestData.ValidInventoryLinkingMovementRequestXML).withHeaders(
          XBadgeIdentifier -> XBadgeIdentifierHeaderValueAsString,
          XClientId -> TestXClientId
        )
        when(mockHeaderValidator.validateHeaders(any[Request[AnyContent]])).thenReturn(validationResult)

        val actualResult: Result = await(actionBuilderValidator.invokeBlock(request, blockReturningOk))

        actualResult shouldBe expectedResult
      }
    }
  }
}
