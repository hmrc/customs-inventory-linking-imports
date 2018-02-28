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

import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderNames.XConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.services.{MessageSender, RequestInfoGenerator}
import uk.gov.hmrc.customs.inventorylinking.imports.services.XmlValidationErrorsMapper
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, SAXException}

abstract class ImportController @Inject()(configProvider: ServiceConfigProvider,
                                 requestInfoGenerator: RequestInfoGenerator,
                                 messageSender: MessageSender,
                                 configName: String,
                                 logger: CdsLogger) extends BaseController {


  def process(): Action[AnyContent] = Action.async { implicit request =>

    def addConversationIdHeader(r: Result, conversationId: String) = {
      r.withHeaders(XConversationId -> conversationId)
    }

    def recover: PartialFunction[Throwable, Future[Result]] = {
      case NonFatal(saxe: SAXException) =>
        logger.debug("XML processing error", saxe)
        Future.successful(
          ErrorResponse.ErrorGenericBadRequest.withErrors(
            XmlValidationErrorsMapper.toResponseContents(saxe): _*).XmlResult)

      case NonFatal(e) =>
        logger.debug("Something went wrong", e)
        Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
    }

    val body = request.body.asXml.getOrElse(NodeSeq.Empty)
    val requestInfo = requestInfoGenerator.newRequestInfo
    val headers = request.headers.toSimpleMap

    messageSender.send(body, requestInfo, headers, configProvider.getConfig(configName)).
      map(_ => Accepted).
      recoverWith(recover).
      map(r => addConversationIdHeader(r, requestInfo.conversationId.toString))
  }


}

@Singleton
class GoodsArrivalController @Inject()(configProvider: ServiceConfigProvider,
                                       requestInfoGenerator: RequestInfoGenerator,
                                       messageSender: MessageSender,
                                       logger: CdsLogger)
  extends ImportController(configProvider, requestInfoGenerator, messageSender, ConfigNames.GoodsArrivalConfig, logger) {

  def post(id: String): Action[AnyContent] = {
    super.process()
  }
}

@Singleton
class ValidateMovementController @Inject()(configProvider: ServiceConfigProvider,
                                           requestInfoGenerator: RequestInfoGenerator,
                                           messageSender: MessageSender,
                                           logger: CdsLogger)
  extends ImportController(configProvider, requestInfoGenerator, messageSender, ConfigNames.ValidateMovementConfig, logger) {

  def post(): Action[AnyContent] = {
     super.process()
  }

}

