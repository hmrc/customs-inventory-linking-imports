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
import play.api.mvc._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{RequestData, ValidatedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future

@Singleton
class ValidateAndExtractHeadersAction @Inject()(validator: HeaderValidator, logger: CdsLogger) extends ActionRefiner[Request, ValidatedRequest] {

  private val actionName = this.getClass.getSimpleName

  override def refine[A](inputRequest: Request[A]): Future[Either[Result, ValidatedRequest[A]]] = Future.successful {
    implicit val r: Request[A] = inputRequest
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    implicit val headers = inputRequest.headers //TODO not needed

    validator.validateHeaders(inputRequest, logger) match { //TODO pass in logger on creation of headervalidator
      case Left(a) => Left(a.XmlResult)
      case Right(_) => {
        val requestData = new RequestData(inputRequest.asInstanceOf[Request[AnyContent]], hc)
        val validatedRequest = ValidatedRequest(requestData, inputRequest)
        Right(validatedRequest)
      }
    }
  }
}