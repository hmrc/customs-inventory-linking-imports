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

package uk.gov.hmrc.customs.inventorylinking.imports.controllers

import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import play.api.mvc.{ActionBuilder, Headers, Request, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorAcceptHeaderInvalid, ErrorContentTypeHeaderInvalid, ErrorGenericBadRequest, ErrorInternalServerError}
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderNames

import scala.concurrent.Future

trait HeaderValidator {

  def validateHeaders(): ActionBuilder[Request] = new ActionBuilder[Request] {

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      implicit val headers = request.headers
      if (!hasAccept) {
        Future.successful(ErrorAcceptHeaderInvalid.XmlResult)
      } else if (!hasContentType) {
        Future.successful(ErrorContentTypeHeaderInvalid.XmlResult)
      } else if (!hasXClientId) {
        Future.successful(ErrorInternalServerError.XmlResult)
      } else if (!hasXBadgeIdentifier) {
        Future.successful(ErrorGenericBadRequest.XmlResult)
      } else {
        block(request)
      }
    }
  }

  private lazy val validAcceptHeaders = Seq("application/vnd.hmrc.1.0+xml")
  private lazy val validContentTypeHeaders = Seq(MimeTypes.XML, MimeTypes.XML + "; charset=utf-8")
  private lazy val xClientIdRegex = "^\\S+$".r
  private lazy val xBadgeIdentifierRegex = "^[0-9A-Za-z]{1,12}$".r

  private def hasAccept(implicit h: Headers) = h.get(ACCEPT).fold(false)(validAcceptHeaders.contains(_))

  private def hasContentType(implicit h: Headers) = h.get(CONTENT_TYPE).fold(false)(h => validContentTypeHeaders.contains(h.toLowerCase()))

  private def hasXClientId(implicit h: Headers) = h.get(HeaderNames.XClientId).fold(false)(xClientIdRegex.findFirstIn(_).nonEmpty)

  private def hasXBadgeIdentifier(implicit h: Headers) = h.get(HeaderNames.XBadgeIdentifier).fold(false)(xBadgeIdentifierRegex.findFirstIn(_).nonEmpty)

}
