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

package uk.gov.hmrc.customs.inventorylinking.imports.connectors

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.api.common.config.ServiceConfig
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.HeaderNames.{XBadgeIdentifier, XClientId}
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestInfo
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator

import scala.xml.NodeSeq

//TODO: maybe this can be reused for other messages?
@Singleton
class OutgoingRequestBuilder @Inject()(payloadDecorator: PayloadDecorator) {

  def build(config: ServiceConfig, requestInfo: RequestInfo, headers: Map[String, String], body: NodeSeq): OutgoingRequest = {
    val clientId = headers.getOrElse(XClientId, "")
    val xBadgeIdentifierValue = headers.getOrElse(XBadgeIdentifier, "")

    OutgoingRequest(
      config,
      payloadDecorator.wrap(body, requestInfo, clientId, xBadgeIdentifierValue),
      requestInfo)
  }
}
