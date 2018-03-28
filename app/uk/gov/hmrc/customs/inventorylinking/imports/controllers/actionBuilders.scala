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
import play.api.mvc._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ExtractedHeaders, RequestData, ValidatedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future
import scala.xml.NodeSeq

// TODO: use ImportsLogger
@Singleton
class ValidateAndExtractHeadersAction @Inject()(validator: HeaderValidator, logger: CdsLogger) extends ActionRefiner[Request, ValidatedRequest] {

  //TODO: add logging
  private val actionName = this.getClass.getSimpleName

  override def refine[A](inputRequest: Request[A]): Future[Either[Result, ValidatedRequest[A]]] = Future.successful {
    implicit val r: Request[A] = inputRequest
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    implicit val headers = inputRequest.headers //TODO not needed

    validator.validateHeaders(inputRequest) match { //TODO pass in logger on creation of header validator
      case Left(result) => Left(result.XmlResult)
      case Right(extractedHeaders) => {
        val requestData = createData(extractedHeaders, inputRequest.asInstanceOf[Request[AnyContent]])
        val validatedRequest = ValidatedRequest(requestData, inputRequest)
        Right(validatedRequest)
      }
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