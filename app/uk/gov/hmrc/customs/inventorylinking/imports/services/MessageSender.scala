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

import javax.inject.{Inject, Singleton}
import play.api.mvc.AnyContent
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ApiSubscriptionFieldsConnector, ImportsConnector, OutgoingRequestBuilder}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class MessageSender @Inject()(apiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector,
                              outgoingRequestBuilder: OutgoingRequestBuilder,
                              goodsArrivalXmlValidationService: GoodsArrivalXmlValidationService,
                              validateMovementXmlValidationService: ValidateMovementXmlValidationService,
                              connector: ImportsConnector,
                              logger: ImportsLogger) {

  def send(messageType: ImportsMessageType)(implicit validatedRequest: ValidatedRequest[AnyContent]): Future[HttpResponse] = {
    for {
      clientSubscriptionId <- apiSubscriptionFieldsConnector.getClientSubscriptionId()
      outgoingRequest = outgoingRequestBuilder.build(messageType, validatedRequest, clientSubscriptionId)
      result <- connector.post(outgoingRequest)
    } yield result
  }
}
