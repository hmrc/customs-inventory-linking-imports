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

package uk.gov.hmrc.customs.inventorylinking.imports.model

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.HeaderNames.ACCEPT
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.Version1AcceptHeaderValue
import uk.gov.hmrc.http.HeaderCarrier

import scala.xml.NodeSeq


case class RequestDataWrapper(request: Request[AnyContent], headerCarrier: HeaderCarrier) {

  private val NotFound = "NOT_FOUND"

  lazy val badgeIdentifier = Some("").getOrElse("") // TODO RM

  lazy val conversationId: String = UUID.randomUUID().toString

  lazy val correlationId: String = UUID.randomUUID().toString

  lazy val dateTime = DateTime.now(DateTimeZone.UTC)

  lazy val body: NodeSeq = request.body.asXml.getOrElse(NodeSeq.Empty)

  lazy val headers: HeaderMap = request.headers.toSimpleMap

  lazy val requestedApiVersion: String = getVersionByAcceptHeader(request.headers.get(ACCEPT))

  lazy val clientId: String = request.headers.get(HeaderConstants.XClientId).getOrElse(NotFound)

  private val versionsByAcceptHeader: HeaderMap = Map(
    Version1AcceptHeaderValue -> "1.0")

  private def getVersionByAcceptHeader(maybeAcceptHeader: Option[String]) = {
    maybeAcceptHeader.fold(NotFound){ accept =>
      versionsByAcceptHeader.getOrElse(accept, NotFound)
    }
  }
}
