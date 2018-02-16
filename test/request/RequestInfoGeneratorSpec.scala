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

package request

import java.util.UUID

import org.joda.time.DateTimeZone
import org.scalatest.{AsyncTestSuite, Matchers, WordSpecLike}
import uk.gov.hmrc.customs.api.common.config.ServiceConfig
import uk.gov.hmrc.customs.inventorylinking.imports.request.{RequestInfo, RequestInfoGenerator}

import scala.concurrent.Future
import scala.xml.Elem

class RequestInfoGeneratorSpec extends WordSpecLike with Matchers {

  trait NewRequestInfo extends AsyncTestSuite {
    private val bearerToken: String = "token"
    val config = ServiceConfig("url", Some(bearerToken), "env")
    val body: Elem = <mdgRequest></mdgRequest>
    val defaultUuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    private val requestInfoGenerator = new RequestInfoGenerator

    val request: Future[RequestInfo] = requestInfoGenerator.newRequestInfo
  }

  "buildRequest" should {
    "creates conversationId" in new NewRequestInfo {
      request.map(r => r.conversationId shouldNot be(defaultUuid))
    }

    "creates correlationId" in new NewRequestInfo {
      request.map(r => r.getClass shouldNot be(defaultUuid))
    }

    "correlationId is not the same as conversationId" in new NewRequestInfo {
      request.map(r => r.conversationId shouldNot be(r.conversationId))
    }

    "dateTime is created in utc" in new NewRequestInfo {
      request.map(r => r.dateTime.getZone shouldBe DateTimeZone.UTC)
    }
  }
}
