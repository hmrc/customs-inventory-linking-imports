/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.inventorylinking.imports.logging.LoggingHelper
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ConversationIdRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ClientId, HeaderConstants}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._

class LoggingHelperSpec extends UnitSpec with MockitoSugar {

  private def expectedMessage(message: String) = s"[conversationId=${ValidConversationId.toString}]" +
    s"[clientId=some-client-id][badgeIdentifier=BADGEID123] $message"
  private val requestMock = mock[Request[_]]
  private val conversationIdRequest =
    ConversationIdRequest(
      ValidConversationId,
      FakeRequest().withHeaders(
        CONTENT_TYPE -> "A",
        ACCEPT -> "B",
        HeaderConstants.XConversationId -> "C",
        HeaderConstants.XClientId -> "D",
        "IGNORE" -> "IGNORE"
      )
    )
  private val validatedHeadersRequest = ValidatedHeadersRequest(ValidBadgeIdentifier, ValidConversationId, Some(ValidCorrelationIdHeader), ValidSubmitterIdentifierHeader, ClientId("some-client-id"), requestMock)

  "LoggingHelper" should {


    "testFormatInfo" in {
      LoggingHelper.formatInfo("Info message", validatedHeadersRequest) shouldBe expectedMessage("Info message")
    }

    "testFormatInfo with authorisation" in {
      LoggingHelper.formatInfo("Info message", validatedHeadersRequest.toAuthorisedRequest) shouldBe expectedMessage("Info message")
    }

    "testFormatError" in {
      LoggingHelper.formatError("Error message", validatedHeadersRequest) shouldBe expectedMessage("Error message")
    }

    "testFormatWarn" in {
      LoggingHelper.formatWarn("Warn message", validatedHeadersRequest) shouldBe expectedMessage("Warn message")
    }

    "testFormatDebug" in {
      LoggingHelper.formatDebug("Debug message", validatedHeadersRequest) shouldBe expectedMessage("Debug message")
    }

    "testFormatDebugFull" in {
      LoggingHelper.formatDebugFull("Debug message.", conversationIdRequest) shouldBe s"[conversationId=$ConversationIdValue] Debug message. headers=Map(Accept -> B, X-Client-ID -> D, Content-Type -> A, X-Conversation-ID -> C)"
    }
  }

}
