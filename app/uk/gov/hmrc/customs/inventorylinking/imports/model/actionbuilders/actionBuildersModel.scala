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

package uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders

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
      eh.badgeIdentifier,
      cir.conversationId,
      eh.correlationIdHeader,
      eh.submitterIdentifier,
      eh.clientId,
      cir.request
    )
  }

  implicit class ValidatedHeadersRequestOps[A](val vhr: ValidatedHeadersRequest[A]) extends AnyVal {

    def toAuthorisedRequest: AuthorisedRequest[A] = AuthorisedRequest(
      vhr.badgeIdentifier,
      vhr.conversationId,
      vhr.correlationIdHeader,
      vhr.submitterIdentifier,
      vhr.clientId,
      vhr.request
    )
  }

  implicit class AuthorisedRequestOps[A](val ar: AuthorisedRequest[A]) extends AnyVal {
    def toValidatedPayloadRequest(xmlBody: NodeSeq): ValidatedPayloadRequest[A] = ValidatedPayloadRequest(
      ar.badgeIdentifier,
      ar.conversationId,
      ar.correlationIdHeader,
      ar.submitterIdentifier,
      ar.clientId,
      xmlBody,
      ar.request
    )
  }
}

trait HasConversationId {
  val conversationId: ConversationId
}

trait ExtractedHeaders {
  val badgeIdentifier: BadgeIdentifier
  val clientId: ClientId
  val correlationIdHeader: Option[CorrelationIdHeader]
  val submitterIdentifier: SubmitterIdentifier
}

trait HasXmlBody {
  val xmlBody: NodeSeq
}

case class ExtractedHeadersImpl(
  badgeIdentifier: BadgeIdentifier,
  clientId: ClientId,
  correlationIdHeader: Option[CorrelationIdHeader],
  submitterIdentifier: SubmitterIdentifier
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
  request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId

// Available after ValidatedHeadersAction builder
case class ValidatedHeadersRequest[A](
                                       badgeIdentifier: BadgeIdentifier,
                                       conversationId: ConversationId,
                                       correlationIdHeader: Option[CorrelationIdHeader],
                                       submitterIdentifier: SubmitterIdentifier,
                                       clientId: ClientId,
                                       request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with ExtractedHeaders

// Available after Authorise action builder
case class AuthorisedRequest[A](
                                 badgeIdentifier: BadgeIdentifier,
                                 conversationId: ConversationId,
                                 correlationIdHeader: Option[CorrelationIdHeader],
                                 submitterIdentifier: SubmitterIdentifier,
                                 clientId: ClientId,
                                 request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with ExtractedHeaders

// Available after ValidatedPayloadAction builder
case class ValidatedPayloadRequest[A](
                                       badgeIdentifier: BadgeIdentifier,
                                       conversationId: ConversationId,
                                       correlationIdHeader: Option[CorrelationIdHeader],
                                       submitterIdentifier: SubmitterIdentifier,
                                       clientId: ClientId,
                                       xmlBody: NodeSeq,
                                       request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with ExtractedHeaders with HasXmlBody
