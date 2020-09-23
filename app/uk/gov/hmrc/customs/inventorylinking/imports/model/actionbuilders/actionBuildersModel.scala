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

package uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders

import java.time.ZonedDateTime

import play.api.mvc.{Request, Result, WrappedRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants._
import uk.gov.hmrc.customs.inventorylinking.imports.model._

import scala.xml.NodeSeq

object ActionBuilderModelHelper {

  implicit class AddConversationId(val result: Result) extends AnyVal {
    def withConversationId(implicit c: HasConversationId): Result = {
      result.withHeaders(XConversationId -> c.conversationId.toString)
    }
  }

  implicit class CorrelationIdsRequestOps[A](val cir: ConversationIdRequest[A]) extends AnyVal {
    def toValidatedHeadersRequest(eh: ExtractedHeaders): ValidatedHeadersRequest[A] = ValidatedHeadersRequest(
      eh.maybeBadgeIdentifier,
      cir.conversationId,
      cir.start,
      eh.requestedApiVersion,
      eh.maybeCorrelationIdHeader,
      eh.maybeSubmitterIdentifier,
      eh.clientId,
      cir.request
    )
  }

  implicit class ValidatedHeadersRequestOps[A](val vhr: ValidatedHeadersRequest[A]) extends AnyVal {

    def toAuthorisedRequest: AuthorisedRequest[A] = AuthorisedRequest(
      vhr.maybeBadgeIdentifier,
      vhr.conversationId,
      vhr.start,
      vhr.requestedApiVersion,
      vhr.maybeCorrelationIdHeader,
      vhr.maybeSubmitterIdentifier,
      vhr.clientId,
      vhr.request
    )
  }

  implicit class AuthorisedRequestOps[A](val ar: AuthorisedRequest[A]) extends AnyVal {
    def toValidatedPayloadRequest(xmlBody: NodeSeq): ValidatedPayloadRequest[A] = ValidatedPayloadRequest(
      ar.maybeBadgeIdentifier,
      ar.conversationId,
      ar.start,
      ar.requestedApiVersion,
      ar.maybeCorrelationIdHeader,
      ar.maybeSubmitterIdentifier,
      ar.clientId,
      xmlBody,
      ar.request
    )
  }
}

trait HasConversationId {
  val conversationId: ConversationId
}

trait HasApiVersion {
  val requestedApiVersion: ApiVersion
}

trait ExtractedHeaders {
  val maybeBadgeIdentifier: Option[BadgeIdentifier]
  val clientId: ClientId
  val requestedApiVersion: ApiVersion
  val maybeCorrelationIdHeader: Option[CorrelationIdHeader]
  val maybeSubmitterIdentifier: Option[SubmitterIdentifier]
}

trait HasXmlBody {
  val xmlBody: NodeSeq
}

case class ExtractedHeadersImpl( requestedApiVersion: ApiVersion,
                                 maybeBadgeIdentifier: Option[BadgeIdentifier],
                                 clientId: ClientId,
                                 maybeCorrelationIdHeader: Option[CorrelationIdHeader],
                                 maybeSubmitterIdentifier: Option[SubmitterIdentifier]
) extends ExtractedHeaders

/*
 * We need multiple WrappedRequest classes to reflect additions to context during the request processing pipeline.
 *
 * There is some repetition in the WrappedRequest classes, but the benefit is we get a flat structure for our data
 * items, reducing the number of case classes and making their use much more convenient, rather than deeply nested stuff
 * eg `r.badgeIdentifier` vs `r.requestData.badgeIdentifier`
 */

// Available after ConversationIdAction action builder
case class ConversationIdRequest[A](
  conversationId: ConversationId,
  start: ZonedDateTime,
  request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId

// Available after ShutterCheckAction
case class ApiVersionRequest[A](
  conversationId: ConversationId,
  start: ZonedDateTime,
  requestedApiVersion: ApiVersion,
  request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with HasApiVersion

// Available after ValidatedHeadersAction builder
case class ValidatedHeadersRequest[A](
                                       maybeBadgeIdentifier: Option[BadgeIdentifier],
                                       conversationId: ConversationId,
                                       start: ZonedDateTime,
                                       requestedApiVersion: ApiVersion,
                                       maybeCorrelationIdHeader: Option[CorrelationIdHeader],
                                       maybeSubmitterIdentifier: Option[SubmitterIdentifier],
                                       clientId: ClientId,
                                       request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with HasApiVersion with ExtractedHeaders

// Available after Authorise action builder
case class AuthorisedRequest[A](
                                 maybeBadgeIdentifier: Option[BadgeIdentifier],
                                 conversationId: ConversationId,
                                 start: ZonedDateTime,
                                 requestedApiVersion: ApiVersion,
                                 maybeCorrelationIdHeader: Option[CorrelationIdHeader],
                                 maybeSubmitterIdentifier: Option[SubmitterIdentifier],
                                 clientId: ClientId,
                                 request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with HasApiVersion with ExtractedHeaders

// Available after ValidatedPayloadAction builder
case class ValidatedPayloadRequest[A](
                                       maybeBadgeIdentifier: Option[BadgeIdentifier],
                                       conversationId: ConversationId,
                                       start: ZonedDateTime,
                                       requestedApiVersion: ApiVersion,
                                       maybeCorrelationIdHeader: Option[CorrelationIdHeader],
                                       maybeSubmitterIdentifier: Option[SubmitterIdentifier],
                                       clientId: ClientId,
                                       xmlBody: NodeSeq,
                                       request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with HasApiVersion with ExtractedHeaders with HasXmlBody
