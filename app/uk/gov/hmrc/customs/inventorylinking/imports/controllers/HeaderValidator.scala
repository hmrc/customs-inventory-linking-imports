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
import play.api.mvc.Headers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorAcceptHeaderInvalid, ErrorContentTypeHeaderInvalid, ErrorGenericBadRequest, ErrorInternalServerError}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.{Version1AcceptHeaderValue, XBadgeIdentifier, XClientId}
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestDataWrapper

trait HeaderValidator {

  def validateHeaders[A](implicit rdWrapper: RequestDataWrapper, logger: ImportsLogger): Either[ErrorResponse, Unit] = {
    implicit val headers = rdWrapper.request.headers

    lazy val maybeAccept = headers.get(ACCEPT)
    lazy val maybeContentType = headers.get(CONTENT_TYPE)
    lazy val maybeXClientId = headers.get(XClientId)
    lazy val maybeXBadgeIdentifier = headers.get(XBadgeIdentifier)

    if (!hasAccept(headers)) {
      logger.error(s"$ACCEPT header was invalid: ${maybeAccept.getOrElse("")}")
      Left(ErrorAcceptHeaderInvalid)
    } else if (!hasContentType(headers)) {
      logger.error(s"$CONTENT_TYPE header was invalid: ${maybeContentType.getOrElse("")}")
      Left(ErrorContentTypeHeaderInvalid)
    } else if (!hasXClientId(headers)) {
      logger.error(s"$XClientId header was invalid: ${maybeXClientId.getOrElse("")}")
      Left(ErrorInternalServerError)
    } else if (!hasXBadgeIdentifier(headers)) {
        logger.error(s"$XBadgeIdentifier header was invalid: ${maybeXBadgeIdentifier.getOrElse("")}")
        Left(ErrorGenericBadRequest)
    } else {
      logger.debug(
        s"$ACCEPT header passed validation: ${maybeAccept}\n"
      + s"$CONTENT_TYPE header passed validation: ${maybeContentType}\n"
      + s"$XClientId header passed validation: ${maybeXClientId}\n"
      + s"$XBadgeIdentifier header passed validation: ${maybeXBadgeIdentifier}")

      Right()
    }
  }

  private lazy val validAcceptHeaders = Seq(Version1AcceptHeaderValue)
  private lazy val validContentTypeHeaders = Seq(MimeTypes.XML + ";charset=utf-8", MimeTypes.XML + "; charset=utf-8")
  private lazy val xClientIdRegex = "^\\S+$".r
  private lazy val xBadgeIdentifierRegex = "^[0-9A-Z]{6,12}$".r

  private def hasAccept(implicit h: Headers) = h.get(ACCEPT).fold(false)(validAcceptHeaders.contains(_))

  private def hasContentType(implicit h: Headers) = h.get(CONTENT_TYPE).fold(false)(h => validContentTypeHeaders.contains(h.toLowerCase()))

  private def hasXClientId(implicit h: Headers) = h.get(XClientId).fold(false)(xClientIdRegex.findFirstIn(_).nonEmpty)

  private def hasXBadgeIdentifier(implicit h: Headers) = h.get(XBadgeIdentifier).fold(false)(xBadgeIdentifierRegex.findFirstIn(_).nonEmpty)

}
