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

import javax.inject.{Inject, Singleton}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.Status
import play.api.mvc.{ActionRefiner, _}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.{AuthProviders, AuthorisationException, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.UnauthorizedCode
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.XConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.services.{GoodsArrivalXmlValidationService, ValidateMovementXmlValidationService, XmlValidationErrorsMapper}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, SAXException}

@Singleton
class ValidateAndExtractHeadersAction @Inject()(validator: HeaderValidator, logger: ImportsLogger) extends ActionRefiner[Request, ValidatedRequest] {

  override def refine[A](inputRequest: Request[A]): Future[Either[Result, ValidatedRequest[A]]] = Future.successful {
    implicit val r: Request[A] = inputRequest
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    validator.validateHeaders(inputRequest) match {
      case Left(result) => Left(result.XmlResult)
      case Right(extractedHeaders) =>
        val requestData = createData(extractedHeaders, inputRequest.asInstanceOf[Request[AnyContent]])
        val validatedRequest = ValidatedRequest(requestData, inputRequest)
        Right(validatedRequest)
    }
  }

  private def createData(extractedHeaders: ExtractedHeaders, request: Request[AnyContent]) = RequestData(
    badgeIdentifier = extractedHeaders.badgeIdentifier,
    conversationId = UUID.randomUUID().toString,
    correlationId = UUID.randomUUID().toString,
    dateTime = DateTime.now(DateTimeZone.UTC),

    //TODO: use body in Play2 WrappedRequest
    body = request.body.asXml.getOrElse(NodeSeq.Empty),

    requestedApiVersion = "1.0",
    clientId = extractedHeaders.xClientId
  )

}

@Singleton
class AuthAction @Inject()(override val authConnector: MicroserviceAuthConnector, logger: ImportsLogger) extends AuthorisedFunctions {

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
      }.recoverWith{
        case NonFatal(authEx: AuthorisationException) =>
          logger.error(s"User is not authorised for this service.")
          logger.debug(s"User is not authorised for this service", authEx)
          Future.successful(Some(addConversationIdHeader(errorResponseUnauthorisedGeneral.XmlResult, validatedRequest.requestData.conversationId)))

        case NonFatal(e) =>
          logger.error(s"An error occurred while processing request.")
          logger.debug(s"An error occurred while processing request ", e)
          Future.successful(Some(addConversationIdHeader(ErrorResponse.ErrorInternalServerError.XmlResult, validatedRequest.requestData.conversationId)))
      }
    }
  }
}

@Singleton
class PayloadValidationAction @Inject()(goodsArrivalXmlValidationService: GoodsArrivalXmlValidationService,
                                         validateMovementXmlValidationService: ValidateMovementXmlValidationService,
                                         logger: ImportsLogger) {

  def validatePayload(importsMessageType: ImportsMessageType): ActionRefiner[ValidatedRequest, ValidatedRequest] = new ActionFilter[ValidatedRequest]() {

    override protected def filter[A](validatedRequest: ValidatedRequest[A]): Future[Option[Result]] = {


      implicit val r = validatedRequest.asInstanceOf[ValidatedRequest[AnyContent]]
      implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)


      def service = importsMessageType match {
        case GoodsArrival => goodsArrivalXmlValidationService
        case ValidateMovement => validateMovementXmlValidationService
      }

      def addConversationIdHeader(r: Result, conversationId: String) = {
        r.withHeaders(XConversationId -> conversationId)
      }

      service.validate(validatedRequest.requestData.body).map(_ => None).recoverWith{
        case NonFatal(saxe: SAXException) =>
          logger.error(s"XML processing error.")
          logger.debug(s"XML processing error.", saxe)
          Future.successful(
            Some(addConversationIdHeader(ErrorResponse.ErrorGenericBadRequest.withErrors(
              XmlValidationErrorsMapper.toResponseContents(saxe): _*).XmlResult, validatedRequest.requestData.conversationId)))
        case NonFatal(e) =>
          logger.error(s"An error occurred while processing request.")
          logger.debug(s"An error occurred while processing request ", e)
          Future.successful(
            Some(addConversationIdHeader(ErrorResponse.ErrorInternalServerError.XmlResult, validatedRequest.requestData.conversationId)))
      }
    }
  }
}


