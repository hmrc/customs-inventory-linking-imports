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
import org.apache.commons.lang3.exception.ExceptionUtils
import play.api.mvc.{Action, AnyContent, Result, _}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.{AuthProviders, AuthorisationException, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.inventorylinking.imports.logging.DeclarationsLogger
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
                                logger: DeclarationsLogger) extends BaseController with HeaderValidator with AuthorisedFunctions {

  private lazy val ErrorResponseUnauthorisedGeneral =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")

  def process(): Action[AnyContent] = Action.async(bodyParser = xmlOrEmptyBody) { implicit request =>
    implicit val rdWrapper: RequestDataWrapper = RequestDataWrapper(request, hc)
    validateHeaders(rdWrapper, logger) match {
      case Right(_) =>
        val eventualResult = authoriseAndSend
        logger.info("Request processed successfully")
        eventualResult

      case Left(errorResponse) =>
        Future.successful(errorResponse.XmlResult)
    }
  }

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) => Right(AnyContentAsXml(xml))
    case _ => Right(AnyContentAsEmpty)
  })

  private def addConversationIdHeader(r: Result, conversationId: String) = {
    r.withHeaders(XConversationId -> conversationId)
  }

  private def handleError(implicit rdWrapper: RequestDataWrapper): PartialFunction[Throwable, Future[Result]] = {

    case NonFatal(saxe: SAXException) =>
      logger.error(s"XML processing error.")
      logger.debug(s"XML processing error ${saxe.getStackTrace}")
      Future.successful(
        ErrorResponse.ErrorGenericBadRequest.withErrors(
          XmlValidationErrorsMapper.toResponseContents(saxe): _*).XmlResult)

    case NonFatal(authEx: AuthorisationException) =>
      logger.error(s"User is not authorised for this service.")
      logger.debug(s"User is not authorised for this service ${authEx.getStackTrace}")
      Future.successful(addConversationIdHeader(ErrorResponseUnauthorisedGeneral.XmlResult, rdWrapper.conversationId))

    case NonFatal(e) =>
      logger.error(s"An error occurred while processing request.")
      logger.debug(s"An error occurred while processing request ${ExceptionUtils.getStackTrace(e)}")
      Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
  }


  private def authoriseAndSend(implicit rdWrapper: RequestDataWrapper): Future[Result] = {
    implicit val hc: HeaderCarrier = rdWrapper.headerCarrier
    def enrolmentForMessageType = importsMessageType match {
      case ValidateMovement => Enrolment("write:customs-il-imports-movement-validation")
      case GoodsArrival => Enrolment("write:customs-il-imports-arrival-notifications")
    }

    authorised(enrolmentForMessageType and AuthProviders(PrivilegedApplication)) {
      messageSender.validateAndSend(importsMessageType).
        map(_ => Accepted)
    }.recoverWith(handleError).
      map(r => addConversationIdHeader(r, rdWrapper.conversationId))
  }
}

@Singleton
class GoodsArrivalController @Inject()(importsConfigService: ImportsConfigService,
                                       authConnector: MicroserviceAuthConnector,
                                       messageSender: MessageSender, logger: DeclarationsLogger)
  extends ImportController(importsConfigService, authConnector, messageSender, GoodsArrival, logger) {

  def post(): Action[AnyContent] = {
    super.process()
  }
}

@Singleton
class ValidateMovementController @Inject()(importsConfigService: ImportsConfigService,
                                           authConnector: MicroserviceAuthConnector,
                                           messageSender: MessageSender,
                                           logger: DeclarationsLogger)
  extends ImportController(importsConfigService, authConnector, messageSender, ValidateMovement, logger) {

  def post(): Action[AnyContent] = {
    super.process()
  }
}
