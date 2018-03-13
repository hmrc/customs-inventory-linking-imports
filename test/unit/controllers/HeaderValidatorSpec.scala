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
import play.api.http.HeaderNames._
import play.api.mvc.{AnyContent, Headers, Request}
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.HeaderValidator
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestDataWrapper
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._

class HeaderValidatorSpec extends UnitSpec with TableDrivenPropertyChecks with MockitoSugar {

  val validator = new HeaderValidator {}

  implicit val rdWrapper = mock[RequestDataWrapper]
  implicit val request = mock[Request[AnyContent]]

  val headersTable =
    Table(
      ("description", "Headers", "Expected response"),
      ("Valid Headers", ValidHeaders, None),
      ("Valid content type XML with no space header", ValidHeaders + (CONTENT_TYPE -> "application/xml;charset=utf-8"), None),
      ("Missing accept header", ValidHeaders - ACCEPT, Some(ErrorAcceptHeaderInvalid)),
      ("Missing content type header", ValidHeaders - CONTENT_TYPE, Some(ErrorContentTypeHeaderInvalid)),
      ("Missing X-Client-ID header", ValidHeaders - XClientIdHeaderName, Some(ErrorInternalServerError)),
      ("Missing X-Badge-Identifier header", ValidHeaders - XBadgeIdentifierHeaderName, Some(ErrorGenericBadRequest)),
      ("Invalid accept header", ValidHeaders + InvalidAcceptHeader, Some(ErrorAcceptHeaderInvalid)),
      ("Invalid content type header JSON header", ValidHeaders + InvalidContentTypeJsonHeader, Some(ErrorContentTypeHeaderInvalid)),
      ("Invalid content type XML without UTF-8 header", ValidHeaders + (CONTENT_TYPE -> "application/xml"), Some(ErrorContentTypeHeaderInvalid)),
      ("Invalid X-Client-ID header", ValidHeaders + InvalidXClientIdHeader, Some(ErrorInternalServerError)),
      ("Invalid X-Badge-Identifier header", ValidHeaders + InvalidXBadgeIdentifier, Some(ErrorGenericBadRequest))
    )

  "HeaderValidatorAction" should {
    forAll(headersTable) { (description, headers, response) =>
      s"$description" in {
        when(request.headers).thenReturn(Headers(headers.toSeq: _*))
        validator.validate shouldBe response
      }
    }
  }
}
