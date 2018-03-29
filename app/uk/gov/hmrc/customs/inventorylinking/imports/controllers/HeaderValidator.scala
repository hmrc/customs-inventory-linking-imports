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

import javax.inject.{Inject, Singleton}

import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import play.api.mvc.{Headers, Request}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorAcceptHeaderInvalid, ErrorContentTypeHeaderInvalid, ErrorGenericBadRequest, ErrorInternalServerError}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.ExtractedHeaders
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.{Version1AcceptHeaderValue, XBadgeIdentifier, XClientId}

@Singleton
class HeaderValidator @Inject() (logger: CdsLogger) {

  private lazy val validAcceptHeaders = Seq(Version1AcceptHeaderValue)
  private lazy val validContentTypeHeaders = Seq(MimeTypes.XML + ";charset=utf-8", MimeTypes.XML + "; charset=utf-8")
  private lazy val xClientIdRegex = "^\\S+$".r
  private lazy val xBadgeIdentifierRegex = "^[0-9A-Z]{6,12}$".r

  def validateHeaders[A](implicit request: Request[A]): Either[ErrorResponse, ExtractedHeaders] = {
    implicit val headers = request.headers

    def hasAccept = validateHeader(ACCEPT, validAcceptHeaders.contains(_), ErrorAcceptHeaderInvalid)

    def hasContentType = validateHeader(CONTENT_TYPE, s => validContentTypeHeaders.contains(s.toLowerCase()), ErrorContentTypeHeaderInvalid)

    def hasXClientId = validateHeader(XClientId, xClientIdRegex.findFirstIn(_).nonEmpty, ErrorInternalServerError)

    def hasXBadgeIdentifier = validateHeader(XBadgeIdentifier, xBadgeIdentifierRegex.findFirstIn(_).nonEmpty, ErrorGenericBadRequest)

    val theResult: Either[ErrorResponse, ExtractedHeaders] = for {
      accept <- hasAccept.right
      contentType <- hasContentType.right
      xClientId <- hasXClientId.right
      xBadgeIdentifier <- hasXBadgeIdentifier.right
    } yield {
      logger.debug(
        s"$ACCEPT header passed validation: $accept\n"
      + s"$CONTENT_TYPE header passed validation: $contentType\n"
      + s"$XClientId header passed validation: $xClientId\n"
      + s"$XBadgeIdentifier header passed validation: $xBadgeIdentifier")
      ExtractedHeaders(xBadgeIdentifier, xClientId)
    }
    theResult
  }

  private def validateHeader(headerName: String, rule: String => Boolean, errorResponse: ErrorResponse)(implicit h: Headers): Either[ErrorResponse, String] = {
    val left = Left(errorResponse)
    val leftWithLog = {
      logger.error(s"$errorResponse ")
      left
    }
    def leftWithLogContainingValue(s: String) = {
      logger.error(s"$errorResponse '$s'")
      left
    }

    h.get(headerName).fold[Either[ErrorResponse, String]]{
      leftWithLog
    }{
      h => if (rule(h)) Right(h) else leftWithLogContainingValue(h)
    }
  }
}

