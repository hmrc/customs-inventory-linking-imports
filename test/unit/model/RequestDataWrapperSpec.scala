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

package unit.model

import java.util.UUID

import org.joda.time.DateTimeZone
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.mvc.{AnyContent, Headers, Request}
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestDataWrapper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class RequestDataWrapperSpec extends UnitSpec with Matchers with MockitoSugar with BeforeAndAfterEach{

  private val requestMock: Request[AnyContent] = mock[Request[AnyContent]]
  private val rdWrapper: RequestDataWrapper = RequestDataWrapper(requestMock, mock[HeaderCarrier])
  private val defaultUuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

  override protected def beforeEach(): Unit = {
    org.mockito.Mockito.reset(requestMock)
  }

  "RequestDataWrapper" should {
    "extract requested api version" in {
      when(requestMock.headers).thenReturn(Headers("ACCEPT" -> "application/vnd.hmrc.1.0+xml"))

      rdWrapper.requestedApiVersion equals "1.0"
    }

    "return NOT_FOUND when invalid requested api version provided" in {
      when(requestMock.headers).thenReturn(Headers("ACCEPT" -> "application/vnd.hmrc.1xx07.0+xml"))

      rdWrapper.requestedApiVersion equals "NOT_FOUND"
    }

    "return NOT_FOUND when no ACCEPT header provided" in {
      when(requestMock.headers).thenReturn(Headers("ACCEPTXXX" -> "application/vnd.hmrc.1xx07.0+xml"))

      rdWrapper.requestedApiVersion equals "NOT_FOUND"
    }

    "return NOT_FOUND when invalid X-Client-ID provided" in {
      when(requestMock.headers).thenReturn(Headers("X-Client-IDXXX" -> "ABC123"))

      rdWrapper.clientId equals "NOT_FOUND"
    }

    "create a conversationId" in {
      rdWrapper.conversationId shouldNot be(defaultUuid)
    }

    "create a correlationId" in {
      rdWrapper.getClass shouldNot be(defaultUuid)
    }

    "correlationId is not the same as conversationId" in{
      rdWrapper.correlationId shouldNot be(rdWrapper.conversationId)
    }

    "dateTime is created in utc" in {
      rdWrapper.dateTime.getZone shouldBe DateTimeZone.UTC
    }

    "return None when badge identifier isn't present" in {
      when(requestMock.headers).thenReturn(Headers("X-Badge-IdentifierXXX" -> "some-badge-id"))
      rdWrapper.badgeIdentifier shouldBe None
    }

    "return Some when badge identifier is present" in {
      when(requestMock.headers).thenReturn(Headers("X-Badge-Identifier" -> "some-badge-id"))
      //val identifier: Option[String] = rdWrapper.badgeIdentifier
      RequestDataWrapper(requestMock, mock[HeaderCarrier]).badgeIdentifier shouldBe Some("some-badge-id")
    }
  }
}
