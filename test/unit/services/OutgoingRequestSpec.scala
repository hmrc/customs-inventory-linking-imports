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

package unit.services

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.OutgoingRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.{RequestData, ValidatedRequest}
import util.TestData
import util.TestData._

class OutgoingRequestSpec extends WordSpecLike with Matchers with MockitoSugar {

  trait validRequest {
    val MDTP: String = "MDTP"

    private val requestData = mock[RequestData]
    private val requestMock = mock[Request[AnyContent]]

    private val rdWrapper: ValidatedRequest[AnyContent] = ValidatedRequest[AnyContent](requestData, requestMock)
    val request = OutgoingRequest(serviceConfig, outgoingBody, rdWrapper)
    when(rdWrapper.rdWrapper.dateTime).thenReturn(TestData.requestDateTime)
    when(rdWrapper.rdWrapper.conversationId).thenReturn(TestData.conversationId.toString)
    when(rdWrapper.rdWrapper.correlationId).thenReturn(TestData.correlationId.toString)
  }

  "headers" should {
    "include accept" in new validRequest {
      request.headers should contain (ACCEPT -> XML)
    }

    "include content-type" in new validRequest {
      request.headers should contain (CONTENT_TYPE -> s"$XML; charset=UTF-8")
    }

    "include X-Forwarded-Host" in new validRequest {
      request.headers should contain ("X-Forwarded-Host" -> MDTP)
    }

    "include Date" in new validRequest {
      request.headers should contain (DATE -> requestDateTimeHttp)
    }

    "include X-Conversation-ID" in new validRequest {
      private val headers: Seq[(String, String)] = request.headers

      headers should contain ("X-Conversation-ID" -> conversationId.toString)
    }

    "include X-Correlation-ID" in new validRequest {
      private val headers: Seq[(String, String)] = request.headers

      headers should contain ("X-Correlation-ID" -> correlationId.toString)
    }

    "include Authorization" in new validRequest {
      request.headers should contain (AUTHORIZATION -> s"Bearer $bearerToken")
    }
  }
}
