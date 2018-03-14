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

import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.logging.{DeclarationsLogger, LoggingHelper}
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestDataWrapper
import uk.gov.hmrc.play.test.UnitSpec
import util.MockitoPassByNameHelper.PassByNameVerifier

class DeclarationsLoggerSpec extends UnitSpec with MockitoSugar {

  implicit val rdWrapper = mock[RequestDataWrapper]
  when(rdWrapper.headers).thenReturn(Map("ACCEPT" -> "Blah", "Authorization" -> "Bearer super-secret-token"))
  when(rdWrapper.conversationId).thenReturn("conversation-id")
  when(rdWrapper.clientId).thenReturn("some-client-id")
  when(rdWrapper.requestedApiVersion).thenReturn("1.0")
  val cdsLoggerMock: CdsLogger = mock[CdsLogger]
  val declarationsLogger = new DeclarationsLogger(cdsLoggerMock)


  private def expectedMessage(message: String, auth: String) = "[conversationId=conversation-id]" +
    "[clientId=some-client-id]" +
    "[requestedApiVersion=1.0]\n" +
    s"[headers=Map(ACCEPT -> Blah, Authorization -> $auth)] $message"

  "DeclarationsLogger" should {

    "testFormatInfo" in {
      declarationsLogger.info("Info message")
      PassByNameVerifier(cdsLoggerMock, "info")
        .withByNameParam[String](expectedMessage("Info message", "value-not-logged"))
        .verify()
    }

    "testFormatError" in {
      declarationsLogger.error("Error message")
      PassByNameVerifier(cdsLoggerMock, "error")
        .withByNameParam[String](expectedMessage("Error message", "value-not-logged"))
        .verify()
    }

    "testFormatWarn" in {
      declarationsLogger.warn("Warn message")
      PassByNameVerifier(cdsLoggerMock, "warn")
        .withByNameParam[String](expectedMessage("Warn message", "value-not-logged"))
        .verify()
    }

    "testFormatDebug" in {
      declarationsLogger.debug("Debug message")
      PassByNameVerifier(cdsLoggerMock, "debug")
        .withByNameParam[String](expectedMessage("Debug message", "Bearer super-secret-token"))
        .verify()
    }
  }
}
