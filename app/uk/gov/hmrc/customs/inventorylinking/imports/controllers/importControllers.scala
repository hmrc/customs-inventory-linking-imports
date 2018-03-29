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

package uk.gov.hmrc.customs.inventorylinking.imports.controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, AnyContent, Result, _}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.XConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.services.{ImportsConfigService, MessageSender, XmlValidationErrorsMapper}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.SAXException

abstract class ImportController(importsConfigService: ImportsConfigService,
                                messageSender: MessageSender,
                                importsMessageType: ImportsMessageType,
                                validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                authAction: AuthAction,
                                logger: ImportsLogger,
                                cdsLogger: CdsLogger //TODO: Change to ImportsLogger
                               ) extends BaseController {

  def process(): Action[AnyContent] =  (Action andThen validateAndExtractHeadersAction andThen authAction.authAction(importsMessageType)).async(bodyParser = xmlOrEmptyBody)  {
    implicit validatedRequest: ValidatedRequest[AnyContent] =>
      //TODO: plug in another Action Builder to validate payload
      //TODO: after that, all this controller does is to call the connector

      messageSender.validateAndSend(importsMessageType).map(_ => addConversationIdHeader(Accepted, validatedRequest.requestData.conversationId)).recoverWith{
        case NonFatal(saxe: SAXException) =>
          logger.error(s"XML processing error.")
          logger.debug(s"XML processing error.", saxe)
          Future.successful(
            addConversationIdHeader(ErrorResponse.ErrorGenericBadRequest.withErrors(
              XmlValidationErrorsMapper.toResponseContents(saxe): _*).XmlResult, validatedRequest.requestData.conversationId)
          )
        case NonFatal(e) =>
          logger.error(s"An error occurred while processing request.")
          logger.debug(s"An error occurred while processing request ", e)
          Future.successful(
            addConversationIdHeader(ErrorResponse.ErrorInternalServerError.XmlResult, validatedRequest.requestData.conversationId)
          )
      }

  }


  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) => Right(AnyContentAsXml(xml))
    case _ => Right(AnyContentAsEmpty)
  })

  private def addConversationIdHeader(r: Result, conversationId: String) = {
    r.withHeaders(XConversationId -> conversationId)
  }

}

@Singleton
class GoodsArrivalController @Inject()(importsConfigService: ImportsConfigService,
                                       messageSender: MessageSender,
                                       validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                       authAction: AuthAction,
                                       logger: ImportsLogger,
                                       cdsLogger: CdsLogger)
  extends ImportController(importsConfigService, messageSender, GoodsArrival, validateAndExtractHeadersAction, authAction, logger, cdsLogger) {

  def post(): Action[AnyContent] = {
    super.process()
  }
}

@Singleton
class ValidateMovementController @Inject()(importsConfigService: ImportsConfigService,
                                           messageSender: MessageSender,
                                           validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                           authAction: AuthAction,
                                           logger: ImportsLogger,
                                           cdsLogger: CdsLogger)
  extends ImportController(importsConfigService, messageSender, ValidateMovement, validateAndExtractHeadersAction, authAction, logger, cdsLogger) {

  def post(): Action[AnyContent] = {
    super.process()
  }
}
