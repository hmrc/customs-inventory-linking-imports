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

import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.model.{FieldsId, ImportsMessageType, RequestInfo, XBadgeIdentifier}
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator

import scala.xml.NodeSeq

@Singleton
class OutgoingRequestBuilder @Inject()(configProvider: ServiceConfigProvider,
                                       payloadDecorator: PayloadDecorator) {

  def build(importsMessageType: ImportsMessageType, requestInfo: RequestInfo, fieldsId: FieldsId, xBadgeIdentifier: XBadgeIdentifier, body: NodeSeq): OutgoingRequest = {
    val clientId = fieldsId.value

    OutgoingRequest(
      configProvider.getConfig(importsMessageType.name),
      payloadDecorator.wrap(body, requestInfo, clientId, xBadgeIdentifier.value, importsMessageType.wrapperRootElementLabel),
      requestInfo)
  }
}
