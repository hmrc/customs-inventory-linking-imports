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

package unit.logging

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.LoggingHelper
import uk.gov.hmrc.customs.inventorylinking.imports.model.{RequestData, ValidatedRequest}
import uk.gov.hmrc.play.test.UnitSpec

class LoggingHelperSpec extends UnitSpec with MockitoSugar {

  private def expectedMessage(message: String, auth: String) = "[conversationId=conversation-id]" +
    "[clientId=some-client-id]" +
    s"[requestedApiVersion=1.0] $message"

  "LoggingHelper" should {

    val requestData = mock[RequestData]
    val requestMock = mock[Request[AnyContent]]

    val rdWrapper = ValidatedRequest[AnyContent](requestData, requestMock)

    when(requestData.conversationId).thenReturn("conversation-id")
    when(requestData.clientId).thenReturn("some-client-id")
    when(requestData.requestedApiVersion).thenReturn("1.0")

    "testFormatInfo" in {
      LoggingHelper.formatInfo("Info message", rdWrapper) shouldBe expectedMessage("Info message", "value-not-logged")
    }

    "testFormatError" in {
      LoggingHelper.formatError("Error message", rdWrapper) shouldBe expectedMessage("Error message", "value-not-logged")
    }

    "testFormatWarn" in {
      LoggingHelper.formatWarn("Warn message", rdWrapper) shouldBe expectedMessage("Warn message", "value-not-logged")
    }

    "testFormatDebug" in {
      LoggingHelper.formatDebug("Debug message", rdWrapper) shouldBe expectedMessage("Debug message", "Bearer super-secret-token")
    }
  }
}
