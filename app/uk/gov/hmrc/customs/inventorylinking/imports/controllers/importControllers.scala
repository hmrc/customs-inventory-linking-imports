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
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.{AuthProviders, AuthorisationException, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.UnauthorizedCode
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderNames.XConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsMessageType, RequestInfo, ValidateMovement}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{ImportsConfigService, MessageSender, RequestInfoGenerator, XmlValidationErrorsMapper}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, SAXException}

abstract class ImportController(importsConfigService: ImportsConfigService,
                                override val authConnector: MicroserviceAuthConnector,
                                requestInfoGenerator: RequestInfoGenerator,
                                messageSender: MessageSender,
                                importsMessageType: ImportsMessageType,
                                logger: CdsLogger) extends BaseController with HeaderValidator with AuthorisedFunctions {

  private lazy val ErrorResponseUnauthorisedGeneral =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")

  def process(): Action[AnyContent] = validateHeaders().async(bodyParser = xmlOrEmptyBody) { implicit request =>

    authoriseAndSend(requestInfoGenerator.newRequestInfo)
  }

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) => Right(AnyContentAsXml(xml))
    case _ => Right(AnyContentAsEmpty)
  })

  private def addConversationIdHeader(r: Result, conversationId: String) = {
    r.withHeaders(XConversationId -> conversationId)
  }

  private def recover: PartialFunction[Throwable, Future[Result]] = {
    case NonFatal(saxe: SAXException) =>
      logger.debug("XML processing error", saxe)
      Future.successful(
        ErrorResponse.ErrorGenericBadRequest.withErrors(
          XmlValidationErrorsMapper.toResponseContents(saxe): _*).XmlResult)

    case NonFatal(_: AuthorisationException) =>
      val requestInfo = requestInfoGenerator.newRequestInfo
      Future.successful(addConversationIdHeader(ErrorResponseUnauthorisedGeneral.XmlResult, requestInfo.conversationId.toString))

    case NonFatal(e) =>
      logger.debug("Something went wrong", e)
      Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)

  }

  private def authoriseAndSend(requestInfo: RequestInfo)(implicit request: Request[AnyContent]): Future[Result] = {
    def enrolmentForMessageType = importsMessageType match {
      case ValidateMovement => Enrolment("write:customs-il-imports-movement-validation")
      case GoodsArrival => Enrolment("write:customs-il-imports-arrival-notifications")
    }

    authorised(enrolmentForMessageType and AuthProviders(PrivilegedApplication)) {
      val body = request.body.asXml.getOrElse(NodeSeq.Empty)
      val requestInfo = requestInfoGenerator.newRequestInfo
      val headers = request.headers.toSimpleMap

      messageSender.validateAndSend(importsMessageType, body, requestInfo, headers).
        map(_ => Accepted)
    }.recoverWith(recover).
      map(r => addConversationIdHeader(r, requestInfo.conversationId.toString))
  }
}

@Singleton
class GoodsArrivalController @Inject()(importsConfigService: ImportsConfigService,
                                       authConnector: MicroserviceAuthConnector,
                                       requestInfoGenerator: RequestInfoGenerator,
                                       messageSender: MessageSender, logger: CdsLogger)
  extends ImportController(importsConfigService, authConnector, requestInfoGenerator, messageSender, GoodsArrival, logger) {

  def post(): Action[AnyContent] = {
    super.process()
  }
}

@Singleton
class ValidateMovementController @Inject()(importsConfigService: ImportsConfigService,
                                           authConnector: MicroserviceAuthConnector,
                                           requestInfoGenerator: RequestInfoGenerator,
                                           messageSender: MessageSender,
                                           logger: CdsLogger)
  extends ImportController(importsConfigService, authConnector, requestInfoGenerator, messageSender, ValidateMovement, logger) {

  def post(): Action[AnyContent] = {
     super.process()
  }
}
