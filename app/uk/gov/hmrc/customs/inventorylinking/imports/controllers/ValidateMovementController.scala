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

import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.RequestInfoGenerator
import uk.gov.hmrc.customs.inventorylinking.imports.mdg.{Connector, MdgRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.request.{RequestInfo, RequestInfoGenerator}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.NodeSeq

class ValidateMovementController @Inject()(connector: Connector,
                                           configProvider: ServiceConfigProvider,
                                           requestInfoGenerator: RequestInfoGenerator)
  extends BaseController {

  def postMessage(id: String): Action[AnyContent] = Action.async { implicit request =>
    def buildMdgRequest(request: Request[AnyContent], config: ServiceConfig, requestInfo: RequestInfo) = {
      Future.successful(
        MdgRequest(
          config,
          request.body.asXml.getOrElse(NodeSeq.Empty),
          requestInfo))
    }

    val config = configProvider.getConfig("mdg-imports")

    (for {
      requestInfo <- requestInfoGenerator.newRequestInfo
      mdgRequest <- buildMdgRequest(request, config, requestInfo)
      result <- connector.postRequestToMdg(mdgRequest)
    } yield result).
      map(_ => Accepted).
      recoverWith {
        case NonFatal(_) => Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
      }
  }
}
