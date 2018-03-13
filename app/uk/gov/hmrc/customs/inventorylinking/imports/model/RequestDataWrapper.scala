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

import play.api.http.HeaderNames.ACCEPT
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.Version1AcceptHeaderValue
import uk.gov.hmrc.http.HeaderCarrier

import scala.xml.NodeSeq


case class RequestDataWrapper(requestInfo: RequestInfo, request: Request[AnyContent], headerCarrier: HeaderCarrier) {


  private val NotFound = "NOT_FOUND"

  lazy val conversationId: String = requestInfo.conversationId.toString

  lazy val body: NodeSeq = request.body.asXml.getOrElse(NodeSeq.Empty)

  lazy val headers: Map[String, String] = request.headers.toSimpleMap

  lazy val requestedApiVersion: String = getVersionByAcceptHeader(request.headers.get(ACCEPT))

  lazy val clientId: String = request.headers.get(HeaderConstants.XClientId).getOrElse(NotFound)

  private val versionsByAcceptHeader: Map[String, String] = Map(
    Version1AcceptHeaderValue -> "1.0")

  private def getVersionByAcceptHeader(maybeAcceptHeader: Option[String]) = {
    maybeAcceptHeader.fold(NotFound){ accept =>
      versionsByAcceptHeader.getOrElse(accept, NotFound)
    }
  }
}
