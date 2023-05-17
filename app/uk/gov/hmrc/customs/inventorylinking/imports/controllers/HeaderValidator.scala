/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc.Headers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorContentTypeHeaderInvalid, ErrorInternalServerError, errorBadRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants._
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ApiVersionRequest, ExtractedHeadersImpl}

@Singleton
class HeaderValidator @Inject() (logger: ImportsLogger) {

  private lazy val validContentTypeHeaders = Seq(MimeTypes.XML + ";charset=utf-8", MimeTypes.XML + "; charset=utf-8")
  private lazy val xClientIdRegex = "^\\S+$".r
  private lazy val xBadgeIdentifierRegex = "^[0-9A-Z]{6,12}$".r
  private lazy val xSubmitterIdentifierHeaderRegex = "^[0-9A-Za-z]{1,17}$".r
  private lazy val errorResponseBadgeIdentifierHeaderInvalid = errorBadRequest(s"${HeaderConstants.XBadgeIdentifier} header is invalid")
  private lazy val errorResponseSubmitterIdentifierHeaderInvalid = errorBadRequest(s"${HeaderConstants.XSubmitterIdentifier} header is invalid")

  def validateHeaders[A](implicit apiVersionRequest: ApiVersionRequest[A]): Either[ErrorResponse, ExtractedHeadersImpl] = {
    implicit val headers: Headers = apiVersionRequest.headers

    def hasContentType = validateHeader(CONTENT_TYPE, s => validContentTypeHeaders.contains(s.toLowerCase()), ErrorContentTypeHeaderInvalid, optional = false)

    def hasXClientId = validateHeader(XClientId, xClientIdRegex.findFirstIn(_).nonEmpty, ErrorInternalServerError, optional = false)

    def hasXBadgeIdentifier = validateHeader(XBadgeIdentifier, xBadgeIdentifierRegex.findFirstIn(_).nonEmpty, errorResponseBadgeIdentifierHeaderInvalid, optional = true)

    def hasXSubmitterIdentifier = validateHeader(XSubmitterIdentifier, xSubmitterIdentifierHeaderRegex.findFirstIn(_).nonEmpty, errorResponseSubmitterIdentifierHeaderInvalid, optional = true)

    val theResult: Either[ErrorResponse, ExtractedHeadersImpl] = for {
      contentType <- hasContentType
      xClientId <- hasXClientId
      xBadgeIdentifier <- hasXBadgeIdentifier
      xSubmitterIdentifier <- hasXSubmitterIdentifier
    } yield {
      val bid = xBadgeIdentifier.fold[Option[BadgeIdentifier]](None)(b => Some(BadgeIdentifier(b)))
      val sid = xSubmitterIdentifier.fold[Option[SubmitterIdentifier]](None)(s => Some(SubmitterIdentifier(s)))
      logger.debug(s"$CONTENT_TYPE header passed validation: $contentType\n"
      + s"$XClientId header passed validation: $xClientId\n"
      + s"$XBadgeIdentifier header passed validation: $xBadgeIdentifier"
      + s"$XSubmitterIdentifier header passed validation: $xSubmitterIdentifier")
      ExtractedHeadersImpl(apiVersionRequest.requestedApiVersion, bid, ClientId(xClientId.get), None, sid) // accept cannot be None
    }
    theResult
  }

  protected def validateHeader[A](headerName: String, rule: String => Boolean, errorResponse: ErrorResponse, optional: Boolean)
                               (implicit apiVersionRequest: ApiVersionRequest[A], headers: Headers): Either[ErrorResponse, Option[String]] = {
    val left = Left(errorResponse)
    def leftWithLog(headerName: String) = {
      logger.error(s"Error - header '$headerName' not present")
      left
    }
    def leftWithLogContainingValue(headerName: String, value: String) = {
      logger.error(s"Error - header '$headerName' value '$value' is not valid")
      left
    }

    headers.get(headerName).fold[Either[ErrorResponse, Option[String]]]{
      if (optional) {
        logger.debug(s"$headerName is optional and empty")
        Right(None)
      } else {
        leftWithLog(headerName)
      }
    }{
      headerValue =>
        if (rule(headerValue)) Right(Some(headerValue)) else leftWithLogContainingValue(headerName, headerValue)
    }
  }
}

