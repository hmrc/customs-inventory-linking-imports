/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.customs.inventorylinking.imports.connectors

import javax.inject.{Inject, Singleton}
import play.mvc.Http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.mvc.Http.MimeTypes.JSON
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.CustomsMetricsRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.services.ImportsConfigService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpException, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsMetricsConnector @Inject()(http: HttpClient,
                                        logger: ImportsLogger,
                                        config: ImportsConfigService)
                                       (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private implicit val hc: HeaderCarrier = HeaderCarrier(
    extraHeaders = Seq(ACCEPT -> JSON, CONTENT_TYPE -> JSON)
  )

  def post[A](request: CustomsMetricsRequest)(implicit hasConversationId: HasConversationId): Future[Unit] = {
    post(request, config.importsConfig.customsMetricsBaseUrl)
  }

  private def post[A](request: CustomsMetricsRequest, url: String)(implicit hasConversationId: HasConversationId): Future[Unit] = {

    logger.debug(s"Sending request to customs metrics. Url: $url Payload:\n${request.toString}")
    http.POST[CustomsMetricsRequest, HttpResponse](url, request).map{ response =>
      response.status match {
        case status if is2xx(status) =>
          logger.debug("customs metrics sent successfully")
        case status => //1xx, 3xx, 4xx, 5xx
          logger.error(s"Call to customs metrics failed. url=$url, status=$status, error=received a non 2XX response")
      }
      ()
    }.recoverWith {
      case httpError: HttpException =>
        logger.error(s"Call to customs metrics failed. url=$url, status=${httpError.responseCode}, error=${httpError.message}")
        Future.failed(new RuntimeException(httpError))
      case e: Throwable =>
        logger.error(s"Call to customs metrics failed. url=$url")
        Future.failed(e)
    }
  }
  
}
