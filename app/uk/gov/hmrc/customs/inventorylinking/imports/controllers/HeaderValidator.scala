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

import com.google.inject.ImplementedBy
import javax.inject.Singleton

import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import play.api.mvc.{Headers, Request}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorAcceptHeaderInvalid, ErrorContentTypeHeaderInvalid, ErrorGenericBadRequest, ErrorInternalServerError}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.ExtractedHeaders
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.{Version1AcceptHeaderValue, XBadgeIdentifier, XClientId}

@ImplementedBy(classOf[HeaderValidatorImpl])
trait HeaderValidator {

  //TODO Move logger to constructor
  def validateHeaders[A](implicit request: Request[A], logger: CdsLogger): Either[ErrorResponse, ExtractedHeaders] = {
    implicit val headers = request.headers

    //TODO: find out why logger.error string is not appearing red in scoverage report
//    if (!hasAccept(headers)) {
//      logger.error(s"$ACCEPT header was invalid: ${maybeAccept.getOrElse("")}")
//      Left(ErrorAcceptHeaderInvalid)
//    } else if (!hasContentType(headers)) {
//      logger.error(s"$CONTENT_TYPE header was invalid: ${maybeContentType.getOrElse("")}")
//      Left(ErrorContentTypeHeaderInvalid)
//    } else if (!hasXClientId(headers)) {
//      logger.error(s"$XClientId header was invalid: ${maybeXClientId.getOrElse("")}")
//      Left(ErrorInternalServerError)
//    } else if (!hasXBadgeIdentifier(headers)) {
//        logger.error(s"$XBadgeIdentifier header was invalid: ${maybeXBadgeIdentifier.getOrElse("")}")
//        Left(ErrorGenericBadRequest)
//    } else {
//      logger.debug(
//        s"$ACCEPT header passed validation: ${maybeAccept}\n"
//      + s"$CONTENT_TYPE header passed validation: ${maybeContentType}\n"
//      + s"$XClientId header passed validation: ${maybeXClientId}\n"
//      + s"$XBadgeIdentifier header passed validation: ${maybeXBadgeIdentifier}")
//
//      Right(())
//    }

    val theResult: Either[ErrorResponse, ExtractedHeaders] = for {
      _ <- hasAccept(headers).right
      _ <- hasContentType(headers).right
      xClientId <- hasXClientId(headers).right
      xbadgeIdentifier <- hasXBadgeIdentifier(headers).right
    } yield ExtractedHeaders(xbadgeIdentifier, xClientId)
    theResult
  }

  private lazy val validAcceptHeaders = Seq(Version1AcceptHeaderValue)
  private lazy val validContentTypeHeaders = Seq(MimeTypes.XML + ";charset=utf-8", MimeTypes.XML + "; charset=utf-8")
  private lazy val xClientIdRegex = "^\\S+$".r
  private lazy val xBadgeIdentifierRegex = "^[0-9A-Z]{6,12}$".r

  //TODO: add logging
  private def hasAccept(implicit h: Headers) = {
    val left = Left(ErrorAcceptHeaderInvalid)
    h.get(ACCEPT).fold[Either[ErrorResponse, String]](left){a => if (validAcceptHeaders.contains(a)) Right((a)) else left}
  }

  private def hasContentType(implicit h: Headers) = {
    val left = Left(ErrorContentTypeHeaderInvalid)
    h.get(CONTENT_TYPE).fold[Either[ErrorResponse, String]](left)(c => if (validContentTypeHeaders.contains(c.toLowerCase())) Right((c)) else left)
  }

  private def hasXClientId(implicit h: Headers) = {
    val left = Left(ErrorInternalServerError)
    h.get(XClientId).fold[Either[ErrorResponse, String]](left)(x => if (xClientIdRegex.findFirstIn(x).nonEmpty) Right((x)) else left)
  }

  private def hasXBadgeIdentifier(implicit h: Headers) = {
    val left = Left(ErrorGenericBadRequest)
    h.get(XBadgeIdentifier).fold[Either[ErrorResponse, String]](left)(b => if (xBadgeIdentifierRegex.findFirstIn(b).nonEmpty) Right((b)) else left)
  }
}

@Singleton
class HeaderValidatorImpl extends HeaderValidator