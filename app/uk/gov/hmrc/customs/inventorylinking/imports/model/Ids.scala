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

package uk.gov.hmrc.customs.inventorylinking.imports.model

import scala.collection.mutable


case class Ids(map: mutable.Map[String, String]) {

  private val requiredHeaders = Map("X-Client-ID" -> "clientId")

  def addDataFromRequestInfo(requestInfo: RequestInfo): mutable.Map[String, String] = {
    map += "conversationId" -> requestInfo.conversationId.toString +
      "correlationId" -> requestInfo.correlationId.toString
  }

  //TODO MC
//  def addDataFromHeaders(headers: SeqOfHeader): mutable.Map[String, String] = {
//
//  }
}

object Ids {
  def empty(): Ids = {
    Ids(mutable.Map.empty[String, String])
  }
}