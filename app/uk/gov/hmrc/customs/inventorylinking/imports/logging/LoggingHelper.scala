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

package uk.gov.hmrc.customs.inventorylinking.imports.logging

import play.api.mvc.AnyContent
import uk.gov.hmrc.customs.inventorylinking.imports.model.ValidatedRequest

object LoggingHelper {

  def formatError(msg: String, validatedRequest: ValidatedRequest[AnyContent]): String = {
    formatMessage(msg, validatedRequest)
  }

  def formatWarn(msg: String, validatedRequest: ValidatedRequest[AnyContent]): String = {
    formatMessage(msg, validatedRequest)
  }

  def formatInfo(msg: String, validatedRequest: ValidatedRequest[AnyContent]): String = {
    formatMessage(msg, validatedRequest)
  }

  def formatDebug(msg: String, validatedRequest: ValidatedRequest[AnyContent]): String = {
    formatMessage(msg, validatedRequest)
  }

  private def formatMessage(msg: String, validatedRequest: ValidatedRequest[AnyContent]): String = {
    s"${format(validatedRequest)} $msg".trim
  }

  private def format(validatedRequest: ValidatedRequest[AnyContent]): String = {
    s"[conversationId=${validatedRequest.requestData.conversationId}][clientId=${validatedRequest.requestData.clientId}][requestedApiVersion=${validatedRequest.requestData.requestedApiVersion}]"
  }

}
