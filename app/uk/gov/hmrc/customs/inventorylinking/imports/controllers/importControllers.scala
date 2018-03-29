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
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.XConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.services.{ImportsConfigService, MessageSender, XmlValidationErrorsMapper}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.SAXException

abstract class ImportController(importsConfigService: ImportsConfigService,
                                override val authConnector: MicroserviceAuthConnector,
                                messageSender: MessageSender,
                                importsMessageType: ImportsMessageType,
                                validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                logger: ImportsLogger,
                                cdsLogger: CdsLogger //TODO: Change to ImportsLogger
                               ) extends BaseController with AuthorisedFunctions {

  private lazy val ErrorResponseUnauthorisedGeneral =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")

  def process(): Action[AnyContent] =  (Action andThen validateAndExtractHeadersAction).async(bodyParser = xmlOrEmptyBody)  {
    implicit request: ValidatedRequest[AnyContent] =>
      authoriseAndSend
  }

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) => Right(AnyContentAsXml(xml))
    case _ => Right(AnyContentAsEmpty)
  })

  private def addConversationIdHeader(r: Result, conversationId: String) = {
    r.withHeaders(XConversationId -> conversationId)
  }

  private def handleError(implicit rdWrapper: ValidatedRequest[AnyContent]): PartialFunction[Throwable, Future[Result]] = {

    case NonFatal(saxe: SAXException) =>
      logger.error(s"XML processing error.")
      logger.debug(s"XML processing error.", saxe)
      Future.successful(
        ErrorResponse.ErrorGenericBadRequest.withErrors(
          XmlValidationErrorsMapper.toResponseContents(saxe): _*).XmlResult)

    case NonFatal(authEx: AuthorisationException) =>
      logger.error(s"User is not authorised for this service.")
      logger.debug(s"User is not authorised for this service", authEx)
      Future.successful(addConversationIdHeader(ErrorResponseUnauthorisedGeneral.XmlResult, rdWrapper.rdWrapper.conversationId))

    case NonFatal(e) =>
      logger.error(s"An error occurred while processing request.")
      logger.debug(s"An error occurred while processing request ", e)
      Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
  }


  private def authoriseAndSend(implicit rdWrapper: ValidatedRequest[AnyContent]): Future[Result] = {
    def enrolmentForMessageType = importsMessageType match {
      case ValidateMovement => Enrolment("write:customs-il-imports-movement-validation")
      case GoodsArrival => Enrolment("write:customs-il-imports-arrival-notifications")
    }

    authorised(enrolmentForMessageType and AuthProviders(PrivilegedApplication)) {
      messageSender.validateAndSend(importsMessageType).
        map(_ => Accepted)
    }.recoverWith(handleError).
      map(r => addConversationIdHeader(r, rdWrapper.rdWrapper.conversationId))
  }
}

@Singleton
class GoodsArrivalController @Inject()(importsConfigService: ImportsConfigService,
                                       authConnector: MicroserviceAuthConnector,
                                       messageSender: MessageSender,
                                       validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                       logger: ImportsLogger,
                                       cdsLogger: CdsLogger)
  extends ImportController(importsConfigService, authConnector, messageSender, GoodsArrival, validateAndExtractHeadersAction, logger, cdsLogger) {

  def post(): Action[AnyContent] = {
    super.process()
  }
}

@Singleton
class ValidateMovementController @Inject()(importsConfigService: ImportsConfigService,
                                           authConnector: MicroserviceAuthConnector,
                                           messageSender: MessageSender,
                                           validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                           logger: ImportsLogger,
                                           cdsLogger: CdsLogger)
  extends ImportController(importsConfigService, authConnector, messageSender, ValidateMovement, validateAndExtractHeadersAction, logger, cdsLogger) {

  def post(): Action[AnyContent] = {
    super.process()
  }
}
