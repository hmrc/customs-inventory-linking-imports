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

import play.api.http.HeaderNames.AUTHORIZATION
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestDataWrapper

object LoggingHelper {


  private val headerOverwriteValue = "value-not-logged"
  private val headersToOverwrite = Set(AUTHORIZATION)

  def formatError(msg: String, rdWrapper: RequestDataWrapper): String = {
    formatMessage(msg, rdWrapper)
  }

  def formatWarn(msg: String, rdWrapper: RequestDataWrapper): String = {
    formatMessage(msg, rdWrapper)
  }

  def formatInfo(msg: String, rdWrapper: RequestDataWrapper): String = {
    formatMessage(msg, rdWrapper)
  }

  def formatDebug(msg: String, rdWrapper: RequestDataWrapper): String = {
    s"${format(rdWrapper, Some(getFilteredHeaders))} $msg".trim
  }


  private def formatMessage(msg: String, rdWrapper: RequestDataWrapper, headersGetter: Option[Map[String, String] => Map[String, String]] = None): String = {
    s"${format(rdWrapper, headersGetter)} $msg".trim
  }


  private def format(rdWrapper: RequestDataWrapper, headersGetter: Option[Map[String, String] => Map[String, String]]): String = {
    s"[conversationId=${rdWrapper.conversationId}]\n[headers=${headersGetter.fold(rdWrapper.headers) { f => f(rdWrapper.headers) }}]"
  }


  def getFilteredHeaders(headers: Map[String, String]): Map[String, String] = headers map {
    case (rewriteHeader, _) if headersToOverwrite.contains(rewriteHeader) => rewriteHeader -> headerOverwriteValue
    case header => header
  }


}
