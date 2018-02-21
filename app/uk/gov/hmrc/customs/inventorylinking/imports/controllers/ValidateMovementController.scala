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

import java.util.UUID
import javax.inject.Inject

import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.inventorylinking.imports.request._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.customs.inventorylinking.imports.request.Headers.xConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.service.XmlValidationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, SAXException}

class ValidateMovementController @Inject()(connector: Connector,
                                           configProvider: ServiceConfigProvider,
                                           requestInfoGenerator: RequestInfoGenerator,
                                           xmlValidationService: XmlValidationService)  extends BaseController {

  def postMessage(id: String): Action[AnyContent] = Action.async { implicit request =>

    def conversationIdHeader(conversationId: UUID) = {
      xConversationId -> conversationId.toString
    }

    def buildOutgoingRequest(request: Request[AnyContent], config: ServiceConfig, requestInfo: RequestInfo) = {
      OutgoingRequest(
        config,
        request.body.asXml.getOrElse(NodeSeq.Empty),
        requestInfo)
    }

    def postToBackendService(request: Request[AnyContent], config: ServiceConfig, requestInfo: RequestInfo) = {
      val outgoingRequest = buildOutgoingRequest(request, config, requestInfo)

      connector.postRequest(outgoingRequest).
        map(_ => Accepted.withHeaders(conversationIdHeader(requestInfo.conversationId))).
        recoverWith {
          case NonFatal(_) => Future.successful(
            ErrorResponse.ErrorInternalServerError.XmlResult.
              withHeaders(conversationIdHeader(requestInfo.conversationId)))
        }
    }

    val payload = request.body.asXml.getOrElse(NodeSeq.Empty)

    val requestInfo = requestInfoGenerator.newRequestInfo

    xmlValidationService.validate(payload).flatMap{
      case _ => postToBackendService(request, configProvider.getConfig("imports"), requestInfo)
    }.recoverWith{
      case NonFatal(saxe: SAXException) =>
              Future.successful(
                ErrorResponse.ErrorGenericBadRequest.withErrors(xmlValidationErrors(saxe): _*).XmlResult.
                  withHeaders(conversationIdHeader(requestInfo.conversationId)))
    }
  }

  private def xmlValidationErrors(saxe: SAXException): Seq[ResponseContents] = {
    @annotation.tailrec
    def loop(thr: Exception, acc: List[ResponseContents]): List[ResponseContents] = {
      val newAcc = ResponseContents("xml_validation_error", thr.getMessage) :: acc
      thr match {
        case saxError: SAXException if Option(saxError.getException).isDefined => loop(saxError.getException, newAcc)
        case _ => newAcc
      }
    }

    loop(saxe, Nil)
  }
}
