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

package util

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.MimeTypes
import play.api.test.Helpers.{ACCEPT, CONTENT_TYPE}
import uk.gov.hmrc.customs.api.common.config.ServiceConfig
import uk.gov.hmrc.customs.inventorylinking.imports.request.RequestInfo

import scala.xml.Elem

object TestData {

  val conversationId: UUID = UUID.fromString("a26a559c-9a1c-42c5-a164-6508beea7749")
  val correlationId: UUID = UUID.fromString("954e2369-3bfa-4aaa-a2a2-c4700e3f71ec")
  val clientId: String = "c9503c3d-6df7-448d-a01b-e623a3b8806d"
  val badgeIdentifier: String = "BadgeId"

  val requestDateTime: DateTime = new DateTime(2017, 6, 8, 13, 55, 0, 0, DateTimeZone.UTC)
  val requestDateTimeHttp: String = "2017-06-08T13:55:00Z"
  val requestInfo: RequestInfo = RequestInfo(conversationId, correlationId, requestDateTime)
  val bearerToken: String = "token"
  val serviceConfig: ServiceConfig = ServiceConfig("url", Some(bearerToken), "env")

  val body: Elem = <payload>payload</payload>

  object Headers {
    val accept: (String, String) = (ACCEPT, "application/vnd.hmrc.1.0+xml")
    val contentType: (String, String) = (CONTENT_TYPE, MimeTypes.XML)
    val xClientIdName: String = "X-Client-Id"
    val xClientId: (String, String) = (xClientIdName, clientId)
    val xBadgeIdentifierName: String = "X-Badge-Identifier"
    val xBadgeIdentifier: (String, String) = (xBadgeIdentifierName, badgeIdentifier)

    val conversationIdHeader: (String, String) = "X-Conversation-Id" -> conversationId.toString

    val validHeaders: Seq[(String, String)] = Seq(accept, contentType, xClientId, xBadgeIdentifier)
  }

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")
}
