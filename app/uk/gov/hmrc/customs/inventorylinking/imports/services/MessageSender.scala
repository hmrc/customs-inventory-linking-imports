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

import java.net.URLEncoder

import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ApiSubscriptionFieldsConnector, ImportsConnector}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left
import scala.util.control.NonFatal
import scala.xml.NodeSeq

@Singleton
class MessageSender @Inject()(apiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector,
                              payloadDecorator: PayloadDecorator,
                              connector: ImportsConnector,
                              dateTimeProvider: DateTimeService,
                              uniqueIdsService: UniqueIdsService,
                              logger: ImportsLogger) {

  private val apiContextEncoded = URLEncoder.encode("customs/inventory-linking-imports", "UTF-8")

  def send[A](importsMessageType: ImportsMessageType)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, Unit]] = {

    futureApiSubFieldsId(vpr.clientId) flatMap {
      case Right(sfId) =>
        callBackend(importsMessageType, sfId)
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  private def futureApiSubFieldsId[A](c: ClientId)
                                     (implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, FieldsId]] = {
    (apiSubscriptionFieldsConnector.getSubscriptionFields(ApiSubscriptionKey(c, apiContextEncoded, VersionOne)) map {
      response: ApiSubscriptionFieldsResponse =>
        Right(FieldsId(response.fieldsId.toString))
    }).recover {
      case NonFatal(e) =>
        logger.error(s"Subscriptions fields lookup call failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def callBackend[A](importsMessageType: ImportsMessageType,
                             subscriptionFieldsId: FieldsId)
                            (implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, Unit]] = {
    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation
    val xmlToSend = preparePayload(vpr.xmlBody, subscriptionFieldsId: FieldsId, correlationId, importsMessageType, dateTime)

    connector.send(importsMessageType, xmlToSend, dateTime, correlationId.uuid).map(_ => Right(())).recover{
      case NonFatal(e) =>
        logger.error(s"Inventory linking call failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def preparePayload[A](xml: NodeSeq, clientId: FieldsId, correlationId: CorrelationId, importsMessageType: ImportsMessageType, dateTime: DateTime)
                               (implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): NodeSeq = {
    logger.debug(s"preparePayload called")
    payloadDecorator.wrap(xml, clientId.value, correlationId.toString, importsMessageType.wrapperRootElementLabel, dateTime)
  }

}
