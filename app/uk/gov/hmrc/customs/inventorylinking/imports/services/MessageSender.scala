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

import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ApiSubscriptionFieldsConnector, ImportsConnector, OutgoingRequestBuilder}
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class MessageSender @Inject()(apiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector,
                              outgoingRequestBuilder: OutgoingRequestBuilder,
                              goodsArrivalXmlValidationService: GoodsArrivalXmlValidationService,
                              validateMovementXmlValidationService: ValidateMovementXmlValidationService,
                              connector: ImportsConnector) {

  private val apiContextEncoded = URLEncoder.encode("customs/inventory-linking-imports", "UTF-8")

  def validateAndSend(messageType: ImportsMessageType)(implicit rdWrapper: RequestDataWrapper): Future[HttpResponse] = {

    val body: NodeSeq = rdWrapper.getBody
    val requestInfo: RequestInfo = rdWrapper.getRequestInfo
    val headers: Map[String, String] = rdWrapper.getHeaders

    def service = messageType match {
      case GoodsArrival => goodsArrivalXmlValidationService
      case ValidateMovement => validateMovementXmlValidationService
    }

    def subsFieldsId(xClientId: XClientId): Future[FieldsId] = {
      val key = ApiSubscriptionKey(xClientId.value, apiContextEncoded, version = "1.0")
      apiSubscriptionFieldsConnector.getSubscriptionFields(key).map(r => FieldsId(r.fieldsId.toString))
    }

    def rdWrapperFromHeaders: Future[(XClientId, XBadgeIdentifier)] = {
      (for {
        xClientId <- headers.get(HeaderNames.XClientId)
        xBadgeIdentifier <- headers.get(HeaderNames.XBadgeIdentifier)
      } yield (XClientId(xClientId), XBadgeIdentifier(xBadgeIdentifier))) match {
        case Some(idsTuple) =>
          Future.successful(idsTuple)
        case _ =>
          Future.failed(new IllegalStateException("Invalid request")) // should never happen as headers are validated
      }
    }

    for {
      _ <- service.validate(body)
      rdWrapperTuple <- rdWrapperFromHeaders
      xClientId = rdWrapperTuple._1
      xBadgeIdentifier = rdWrapperTuple._2
      fieldsId <- subsFieldsId(xClientId)
      outgoingRequest = outgoingRequestBuilder.build(messageType, requestInfo, fieldsId, xBadgeIdentifier, body)
      result <- connector.post(outgoingRequest)
    } yield result
  }

}
