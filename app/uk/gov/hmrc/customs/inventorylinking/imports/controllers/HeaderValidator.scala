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
import uk.gov.hmrc.customs.inventorylinking.imports.logging.DeclarationsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.{Version1AcceptHeaderValue, XBadgeIdentifier, XClientId}
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestDataWrapper

trait HeaderValidator {

  def validateHeaders[A](implicit rdWrapper: RequestDataWrapper, logger: DeclarationsLogger): Either[ErrorResponse, Unit] = {
    implicit val headers = rdWrapper.request.headers
    if (!hasAccept(headers, logger, rdWrapper)) {
      Left(ErrorAcceptHeaderInvalid)
    } else if (!hasContentType(headers, logger, rdWrapper)) {
      Left(ErrorContentTypeHeaderInvalid)
    } else if (!hasXClientId(headers, logger, rdWrapper)) {
      Left(ErrorInternalServerError)
    } else if (!hasXBadgeIdentifier(headers, logger, rdWrapper)) {
      Left(ErrorGenericBadRequest)
    } else {
      Right()
    }
  }

  private lazy val validAcceptHeaders = Seq(Version1AcceptHeaderValue)
  private lazy val validContentTypeHeaders = Seq(MimeTypes.XML + ";charset=utf-8", MimeTypes.XML + "; charset=utf-8")
  private lazy val xClientIdRegex = "^\\S+$".r
  private lazy val xBadgeIdentifierRegex = "^[0-9A-Za-z]{1,12}$".r

  private def hasAccept(implicit h: Headers, logger: DeclarationsLogger, rdWrapper: RequestDataWrapper): Boolean = {
    val maybeHeader = h.get(ACCEPT)
    val hasValidHeader = maybeHeader.fold(false)(validAcceptHeaders.contains(_))
    log(logger, hasValidHeader, ACCEPT, maybeHeader)
    hasValidHeader
  }

  private def hasContentType(implicit h: Headers, logger: DeclarationsLogger, rdWrapper: RequestDataWrapper): Boolean = {
    val maybeHeader = h.get(CONTENT_TYPE)
    val hasValidHeader = maybeHeader.fold(false)(h => validContentTypeHeaders.contains(h.toLowerCase()))
    log(logger, hasValidHeader, CONTENT_TYPE, maybeHeader)
    hasValidHeader
  }

  private def hasXClientId(implicit h: Headers, logger: DeclarationsLogger, rdWrapper: RequestDataWrapper) = {
    val maybeHeader = h.get(XClientId)
    val hasValidHeader = maybeHeader.fold(false)(xClientIdRegex.findFirstIn(_).nonEmpty)
    log(logger, hasValidHeader, XClientId, maybeHeader)
    hasValidHeader
  }

  private def hasXBadgeIdentifier(implicit h: Headers, logger: DeclarationsLogger, rdWrapper: RequestDataWrapper) = {
    val maybeHeader = h.get(XBadgeIdentifier)
    val hasValidHeader = maybeHeader.fold(true)(xBadgeIdentifierRegex.findFirstIn(_).nonEmpty)
    log(logger, hasValidHeader, XBadgeIdentifier, maybeHeader)
    hasValidHeader
  }

  private def log(logger: DeclarationsLogger, validHeader: Boolean, headerName: String, maybeHeaderValue: Option[String])(implicit rdWrapper: RequestDataWrapper): Unit = {
    validHeader match {
      case true => logger.debug(s"$headerName header passed validation: $maybeHeaderValue")
      case false => logger.error(s"$headerName was invalid: $maybeHeaderValue")
    }
  }
}
