/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.mvc.Headers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants._
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ConversationIdRequest, ExtractedHeadersImpl}

@Singleton
class ValidateMovementHeaderValidator @Inject()(logger: ImportsLogger) extends HeaderValidator(logger) {

  private lazy val xCorrelationIdHeaderRegex = "^.{1,36}$".r
  private lazy val errorResponseCorrelationIdHeaderMissing = errorBadRequest(s"${HeaderConstants.XCorrelationId} header is missing or invalid")

  override def validateHeaders[A](implicit conversationIdRequest: ConversationIdRequest[A]): Either[ErrorResponse, ExtractedHeadersImpl] = {

    implicit val headers: Headers = conversationIdRequest.headers

    def hasXCorrelationId = validateHeader(XCorrelationId, xCorrelationIdHeaderRegex.findFirstIn(_).nonEmpty, errorResponseCorrelationIdHeaderMissing, optional = false)

    super.validateHeaders match {
      case Right(b) =>
        val theResult: Either[ErrorResponse, ExtractedHeadersImpl] = for {
          xCorrelationId <- hasXCorrelationId.right
        } yield {
          val cid = xCorrelationId.map( CorrelationIdHeader(_) )
          logger.debug(s"$XCorrelationId header passed validation: $xCorrelationId")
          ExtractedHeadersImpl(b.requestedApiVersion, b.maybeBadgeIdentifier, b.clientId, cid, b.maybeSubmitterIdentifier)
        }
        theResult
      case left => left
    }
  }
}
