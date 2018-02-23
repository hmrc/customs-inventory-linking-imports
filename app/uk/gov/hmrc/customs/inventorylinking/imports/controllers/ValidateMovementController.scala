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

import javax.inject.Inject

import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.request.HeaderNames._
import uk.gov.hmrc.customs.inventorylinking.imports.request._
import uk.gov.hmrc.customs.inventorylinking.imports.xml.XmlValidationErrorsMapper
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, SAXException}

class ValidateMovementController @Inject()(configProvider: ServiceConfigProvider,
                                           requestInfoGenerator: RequestInfoGenerator,
                                           messageSender: ValidateMovementMessageSender)
  extends BaseController {

  def postMessage(id: String): Action[AnyContent] = Action.async { implicit request =>

    def addConversationIdHeader(r: Result, conversationId: String) = {
      r.withHeaders(xConversationId -> conversationId)
    }

    def recover: PartialFunction[Throwable, Future[Result]] = {
      case NonFatal(saxe: SAXException) =>
        Future.successful(
          ErrorResponse.ErrorGenericBadRequest.withErrors(
            XmlValidationErrorsMapper.toResponseContents(saxe): _*).XmlResult)

      case NonFatal(_) => Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
    }

    val body = request.body.asXml.getOrElse(NodeSeq.Empty)
    val requestInfo = requestInfoGenerator.newRequestInfo
    val headers = request.headers.toSimpleMap

    messageSender.send(body, requestInfo, headers, configProvider.getConfig("imports")).
      map(_ => Accepted).
      recoverWith(recover).
      map(r => addConversationIdHeader(r, requestInfo.conversationId.toString))
  }
}
