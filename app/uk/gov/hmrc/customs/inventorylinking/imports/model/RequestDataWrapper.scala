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

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier

import scala.xml.NodeSeq


case class RequestDataWrapper(requestInfo: RequestInfo, request: Request[AnyContent], hc: HeaderCarrier) {

  //TODO MC requestedApiVersion
  def getConversationId: String = requestInfo.conversationId.toString

  def getBody: NodeSeq = request.body.asXml.getOrElse(NodeSeq.Empty)

  def getRequestInfo: RequestInfo = requestInfo

  def getHeaders: Map[String, String] = request.headers.toSimpleMap

  def getHeaderCarrier: HeaderCarrier = hc

}
