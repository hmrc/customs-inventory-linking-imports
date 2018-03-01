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

import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.HeaderNames._
import play.api.mvc.Results._
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.HeaderValidator
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData
import util.TestData.Headers.validHeaders
import util.TestData._

class HeaderValidatorSpec extends UnitSpec with TableDrivenPropertyChecks {

  val validator = new HeaderValidator {}

  val action: Action[AnyContent] = validator.validateHeaders async {
    Ok
  }

  val headersTable =
    Table(
      ("description", "Headers", "Expected response"),
      ("Missing accept header", validHeaders- ACCEPT, ErrorAcceptHeaderInvalid.XmlResult),
      ("Missing content type header", validHeaders - CONTENT_TYPE, ErrorContentTypeHeaderInvalid.XmlResult),
      ("Missing X-Client-ID header", validHeaders - XClientIdHeaderName, ErrorInternalServerError.XmlResult),
      ("Missing X-Badge-Identifier header", validHeaders - XBadgeIdentifierHeaderName, ErrorGenericBadRequest.XmlResult),

      ("Invalid accept header", Map(TestData.InvalidAcceptHeader), ErrorAcceptHeaderInvalid.XmlResult),
      ("Invalid content type header", validHeaders + InvalidContentTypeHeader, ErrorContentTypeHeaderInvalid.XmlResult),
      ("Invalid X-Client-ID header", validHeaders + InvalidXClientIdHeader, ErrorInternalServerError.XmlResult),
      ("Invalid X-Badge-Identifier header", validHeaders + InvalidXBadgeIdentifier, ErrorGenericBadRequest.XmlResult)
    )

  private def requestWithHeaders(headers: Map[String, String]) =
    FakeRequest().withHeaders(headers.toSeq: _*)

  "HeaderValidatorAction" should {
    forAll(headersTable) { (description, headers, response) =>
      s"$description" in {
        await(action.apply(requestWithHeaders(headers))) shouldBe response
      }
    }
  }
}
