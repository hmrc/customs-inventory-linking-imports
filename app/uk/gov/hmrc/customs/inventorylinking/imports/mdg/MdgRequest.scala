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

package uk.gov.hmrc.customs.inventorylinking.imports.mdg

import java.util.UUID

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION, CONTENT_TYPE, DATE}
import play.api.http.MimeTypes._
import uk.gov.hmrc.customs.api.common.config.ServiceConfig

import scala.xml.NodeSeq

case class MdgRequest(service: ServiceConfig,
                      body: NodeSeq,
                      conversationId: UUID = UUID.randomUUID(),
                      correlationId: UUID = UUID.randomUUID(),
                      dateTime: DateTime = DateTime.now(DateTimeZone.UTC)) {

  lazy val bearerToken: String = service.bearerToken.getOrElse("")
  lazy val url: String = service.url

  lazy val headers: Seq[(String, String)] = Seq(
      ACCEPT -> XML,
      CONTENT_TYPE -> XML,
      AUTHORIZATION -> s"Bearer $bearerToken",
      DATE -> dateTime.toString(ISODateTimeFormat.dateTimeNoMillis()),
      "X-Forwarded-Host" -> "MDTP",
      "X-Conversation-Id" -> conversationId.toString,
      "X-Correlation-Id" -> correlationId.toString
    )
}

