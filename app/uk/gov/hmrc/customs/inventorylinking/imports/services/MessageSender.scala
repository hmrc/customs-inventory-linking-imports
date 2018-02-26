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

package uk.gov.hmrc.customs.inventorylinking.imports.services

import javax.inject.Inject

import uk.gov.hmrc.customs.api.common.config.ServiceConfig
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{OutgoingRequestBuilder, ValidateMovementConnector}
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestInfo
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

class MessageSender @Inject()(outgoingRequestBuilder: OutgoingRequestBuilder,
                              xmlValidationService: XmlValidationService,
                              connector: ValidateMovementConnector) {

  def send(body: NodeSeq, requestInfo: RequestInfo, headers: Map[String, String], config: ServiceConfig): Future[HttpResponse] = {
    val outgoingRequest = outgoingRequestBuilder.build(config, requestInfo, headers, body)

    for {
      _ <- xmlValidationService.validate(body)
      result <- connector.post(outgoingRequest)
    } yield result
  }
}