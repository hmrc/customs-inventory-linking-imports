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

import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Headers, Request}
import uk.gov.hmrc.customs.inventorylinking.imports.model.{RequestDataWrapper, RequestInfo}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class RequestDataWrapperSpec extends UnitSpec with Matchers with MockitoSugar with BeforeAndAfterEach{

  private val requestMock: Request[AnyContent] = mock[Request[AnyContent]]

  private val data: RequestDataWrapper = RequestDataWrapper(mock[RequestInfo], requestMock, mock[HeaderCarrier])

  "RequestDataWrapper" should {
    "extract requested api version" in {
      when(requestMock.headers).thenReturn(Headers("ACCEPT" -> "application/vnd.hmrc.1.0+xml"))
      data.requestedApiVersion equals "1.0"
    }

    "return NOT_FOUND when invalid requested api version provided" in {
      when(requestMock.headers).thenReturn(Headers("ACCEPT" -> "application/vnd.hmrc.1xx07.0+xml"))
      data.requestedApiVersion equals "NOT_FOUND"
    }

    "return NOT_FOUND when no ACCEPT header provided" in {
      when(requestMock.headers).thenReturn(Headers("ACCEPTXXX" -> "application/vnd.hmrc.1xx07.0+xml"))
      data.requestedApiVersion equals "NOT_FOUND"
    }

    "return NOT_FOUND when invalid X-Client-ID provided" in {
      when(requestMock.headers).thenReturn(Headers("X-Client-IDXXX" -> "ABC123"))
      data.clientId equals "NOT_FOUND"
    }
  }

  override protected def beforeEach(): Unit = {
    org.mockito.Mockito.reset(requestMock)
  }
}
