/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers._
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.HasConversationId
import util.MockitoPassByNameHelper.PassByNameVerifier

object VerifyLogging {

  def verifyImportsLoggerError(message: String)(implicit logger: ImportsLogger): Unit = {
    verifyImportsLogger("error", message)
  }

  def verifyImportsLogger(method: String, message: String)(implicit logger: ImportsLogger): Unit = {
    PassByNameVerifier(logger, method)
      .withByNameParam(message)
      .withParamMatcher(any[HasConversationId])
      .verify()
  }

  def verifyImportsLoggerThrowable(method: String, message: String)(implicit logger: ImportsLogger): Unit = {
    PassByNameVerifier(logger, method)
      .withByNameParam(message)
      .withByNameParamMatcher(any[Throwable]) // order/position of this matcher is important
      .withParamMatcher(any[HasConversationId])
      .verify()
  }

}
