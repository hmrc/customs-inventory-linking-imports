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

import java.util.UUID
import javax.inject.{Inject, Singleton}

import org.joda.time.{DateTime, DateTimeZone}
import play.api.mvc.{ActionRefiner, _}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.HeaderValidator
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model._

import scala.concurrent.Future

@Singleton
class ValidateAndExtractHeadersAction @Inject()(validator: HeaderValidator, logger: ImportsLogger) extends ActionRefiner[Request, ValidatedRequest] {

  override def refine[A](inputRequest: Request[A]): Future[Either[Result, ValidatedRequest[A]]] = Future.successful {
    implicit val r: Request[A] = inputRequest

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
    requestedApiVersion = "1.0",
    clientId = extractedHeaders.xClientId
  )
}


