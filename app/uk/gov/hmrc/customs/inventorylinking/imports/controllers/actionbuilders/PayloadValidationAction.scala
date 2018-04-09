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

package uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders

import javax.inject.{Inject, Singleton}

import play.api.mvc.{ActionFilter, ActionRefiner, AnyContent, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.XConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsMessageType, ValidateMovement, ValidatedRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{GoodsArrivalXmlValidationService, ValidateMovementXmlValidationService, XmlValidationErrorsMapper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, SAXException}

@Singleton
class PayloadValidationAction @Inject()(goodsArrivalXmlValidationService: GoodsArrivalXmlValidationService,
                                         validateMovementXmlValidationService: ValidateMovementXmlValidationService,
                                         logger: ImportsLogger) {

  def validatePayload(importsMessageType: ImportsMessageType): ActionRefiner[ValidatedRequest, ValidatedRequest] = new ActionFilter[ValidatedRequest]() {

    override protected def filter[A](validatedRequest: ValidatedRequest[A]): Future[Option[Result]] = {

      implicit val r = validatedRequest.asInstanceOf[ValidatedRequest[AnyContent]]

      def service = importsMessageType match {
        case GoodsArrival => goodsArrivalXmlValidationService
        case ValidateMovement => validateMovementXmlValidationService
      }

      def addConversationIdHeader(r: Result, conversationId: String) = {
        r.withHeaders(XConversationId -> conversationId)
      }

      service.validate(r.body.asXml.getOrElse(NodeSeq.Empty)).map(_ => None).recoverWith {
        case NonFatal(saxe: SAXException) =>
          logger.error(s"XML processing error.")
          logger.debug(s"XML processing error.", saxe)
          Future.successful(
            Some(ErrorResponse.ErrorGenericBadRequest.withErrors(
              XmlValidationErrorsMapper.toResponseContents(saxe): _*).XmlResult))
        case NonFatal(e) =>
          logger.error(s"An error occurred while processing request.")
          logger.debug(s"An error occurred while processing request ", e)
          Future.successful(
            Some(ErrorResponse.ErrorInternalServerError.XmlResult))
      }.map(maybeResult => maybeResult.map(r => addConversationIdHeader(r, validatedRequest.requestData.conversationId)))
    }
  }
}
