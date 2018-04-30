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
import play.api.http.Status
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.UnauthorizedCode
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.XConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsMessageType, ValidateMovement, ValidatedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class AuthAction @Inject()(override val authConnector: AuthConnector, logger: ImportsLogger) extends AuthorisedFunctions {

  private val errorResponseUnauthorisedGeneral =
    ErrorResponse(Status.UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")

  def authAction(importsMessageType: ImportsMessageType): ActionRefiner[ValidatedRequest, ValidatedRequest] = new ActionFilter[ValidatedRequest]() {

    override protected def filter[A](validatedRequest: ValidatedRequest[A]): Future[Option[Result]] = {
      implicit val r = validatedRequest.asInstanceOf[ValidatedRequest[AnyContent]]
      implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

      def enrolmentForMessageType = importsMessageType match {
        case ValidateMovement =>
          Enrolment("write:customs-il-imports-movement-validation")
        case GoodsArrival =>
          Enrolment("write:customs-il-imports-arrival-notifications")
      }

      def addConversationIdHeader(r: Result, conversationId: String) = {
        r.withHeaders(XConversationId -> conversationId)
      }

      authorised(enrolmentForMessageType and AuthProviders(PrivilegedApplication)) {
        Future.successful(None)
      }.recoverWith {
        case NonFatal(authEx: AuthorisationException) =>
          logger.error(s"User is not authorised for this service.")
          logger.debug(s"User is not authorised for this service", authEx)
          Future.successful(Some(errorResponseUnauthorisedGeneral.XmlResult))
        case NonFatal(e) =>
          logger.error(s"An error occurred while processing request.")
          logger.debug(s"An error occurred while processing request ", e)
          Future.successful(Some(ErrorResponse.ErrorInternalServerError.XmlResult))
      }.map(maybeResult => maybeResult.map(r => addConversationIdHeader(r, validatedRequest.requestData.conversationId)))
    }
  }
}
