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

import play.api.http.HeaderNames.AUTHORIZATION
import play.api.mvc.{AnyContent, Headers, Request}
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable
import scala.xml.NodeSeq


case class Ids(private val map: mutable.Map[String, String], request: Request[AnyContent], hc: HeaderCarrier) {



  private val headerOverwriteValue = "value-not-logged"
  private val headersToOverwrite = Set(AUTHORIZATION)

  //TODO MC
  def addDataFromHeaders(headers: Headers): Ids = {
    //TODO use overwriteHeaderValues here
    map += "data" -> "from headers"
    this
  }

  def getConversationId: String = ???
  def getBody: NodeSeq = request.body.asXml.getOrElse(NodeSeq.Empty)
  def getRequestInfo: RequestInfo = ???
  def getHeaders: Map[String, String] = request.headers.toSimpleMap
  def getHeaderCarrier: HeaderCarrier = hc

  private def overwriteHeaderValues(headers: SeqOfHeader, overwrittenHeaderNames: Set[String]): SeqOfHeader = {
    headers map {
      case (rewriteHeader, _) if overwrittenHeaderNames.contains(rewriteHeader) => rewriteHeader -> headerOverwriteValue
      case header => header
    }
  }
}

object Ids {
  def fromDataFromRequestInfo(requestInfo: RequestInfo, request: Request[AnyContent], hc: HeaderCarrier): Ids = {
    Ids(mutable.Map("conversationId" -> requestInfo.conversationId.toString +
      "correlationId" -> requestInfo.correlationId.toString), request, hc)
  }
}
