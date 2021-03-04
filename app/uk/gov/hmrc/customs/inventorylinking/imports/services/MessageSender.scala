/*
 * Copyright 2021 HM Revenue & Customs
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
import java.util.UUID

import akka.pattern.CircuitBreakerOpenException
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorInternalServerError
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ApiSubscriptionFieldsConnector, ImportsConnector}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left
import scala.util.control.NonFatal
import scala.xml.NodeSeq

@Singleton
class MessageSender @Inject()(apiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector,
                              payloadDecorator: PayloadDecorator,
                              connector: ImportsConnector,
                              dateTimeProvider: DateTimeService,
                              uniqueIdsService: UniqueIdsService,
                              logger: ImportsLogger
                             )
                             (implicit ex: ExecutionContext) {

  private val apiContextEncoded = URLEncoder.encode("customs/inventory-linking-imports", "UTF-8")
  private val errorResponseServiceUnavailable = errorInternalServerError("This service is currently unavailable")

  def send[A](importsMessageType: ImportsMessageType)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, Unit]] = {

    futureApiSubFieldsId(vpr.clientId) flatMap {
      case Right(asfr) =>
        callBackend(importsMessageType, asfr)
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  private def futureApiSubFieldsId[A](c: ClientId)
                                     (implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, ApiSubscriptionFieldsResponse]] = {
    (apiSubscriptionFieldsConnector.getSubscriptionFields(ApiSubscriptionKey(c, apiContextEncoded, vpr.requestedApiVersion)) map {
      response: ApiSubscriptionFieldsResponse =>
        logger.debug(s"Got a response from api subscription fields $response")
        response.fields.authenticatedEori match {
          case Some(_) =>
            logger.info("Got an eori back from api subscription fields")
            Right(response)
          case None =>
            logger.info("No eori returned from api subscription fields")
            Left(errorInternalServerError("Missing authenticated eori in service lookup").XmlResult.withConversationId)
        }
    }).recover[Either[Result, ApiSubscriptionFieldsResponse]] {
      case NonFatal(e) =>
        logger.error(s"Subscriptions fields lookup call failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def callBackend[A](importsMessageType: ImportsMessageType,
                             apiSubscriptionFieldsResponse: ApiSubscriptionFieldsResponse)
                            (implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, Unit]] = {
    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation
    val xmlToSend = preparePayload(vpr.xmlBody, apiSubscriptionFieldsResponse: ApiSubscriptionFieldsResponse, vpr.maybeCorrelationIdHeader, importsMessageType, dateTime, correlationId.uuid)

    connector.send(importsMessageType, xmlToSend, dateTime, correlationId.uuid).map[Either[Result, Unit]]{
      _ => Right(())
    }.recover{
      case _: CircuitBreakerOpenException =>
        logger.error("unhealthy state entered")
        Left(errorResponseServiceUnavailable.XmlResult)
      case NonFatal(e) =>
        logger.error(s"Inventory linking imports request failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def preparePayload[A](xml: NodeSeq,
                                apiSubscriptionFieldsResponse: ApiSubscriptionFieldsResponse,
                                correlationIdHeader: Option[CorrelationIdHeader],
                                importsMessageType: ImportsMessageType,
                                dateTime: DateTime,
                                correlationId: UUID)
                               (implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): NodeSeq = {
    logger.debug(s"preparePayload called")
    payloadDecorator.wrap(xml, apiSubscriptionFieldsResponse, correlationIdHeader, importsMessageType.wrapperRootElementLabel, dateTime, correlationId)
  }
}
