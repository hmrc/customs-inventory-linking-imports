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

package unit.request

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import uk.gov.hmrc.customs.api.common.config.ServiceConfig
import uk.gov.hmrc.customs.inventorylinking.imports.request.{OutgoingRequest, RequestInfo}

import scala.xml.Elem

class OutgoingRequestSpec extends WordSpecLike with Matchers with MockitoSugar {

  trait validRequest {
    private val dateTime = new DateTime(2017, 6, 8, 13, 55, 0, 0, DateTimeZone.UTC)

    val dateTimeHttp: String = "2017-06-08T13:55:00Z"
    val conversationId: UUID = UUID.fromString("2139b8d1-1875-4f84-8af1-f3ce01965c6f")
    val correlationId: UUID = UUID.fromString("5664704d-af3e-43bc-8c6c-f24ce6970e84")
    val bearerToken: String = "token"
    val MDTP: String = "MDTP"

    private val config = ServiceConfig("url", Some(bearerToken), "env")
    private val body: Elem = <request></request>

    val request = OutgoingRequest(config, body, RequestInfo(conversationId, correlationId, dateTime))
  }

  "headers" should {
    "include accept" in new validRequest {
      request.headers should contain (ACCEPT -> XML)
    }

    "include content-type" in new validRequest {
      request.headers should contain (CONTENT_TYPE -> XML)
    }

    "include X-Forwared-Host" in new validRequest {
      request.headers should contain ("X-Forwarded-Host" -> MDTP)
    }

    "include Date" in new validRequest {
      request.headers should contain (DATE -> dateTimeHttp)
    }

    "include X-Conversation-Id" in new validRequest {
      request.headers should contain ("X-Conversation-Id" -> conversationId.toString)
    }

    "include X-Correlation-Id" in new validRequest {
      request.headers should contain ("X-Correlation-Id" -> correlationId.toString)
    }

    "include Authorization" in new validRequest {
      request.headers should contain (AUTHORIZATION -> s"Bearer $bearerToken")
    }
  }
}
