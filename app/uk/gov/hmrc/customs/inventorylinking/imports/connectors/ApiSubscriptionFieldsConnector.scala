/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ApiSubscriptionFieldsResponse, ApiSubscriptionKey}
import uk.gov.hmrc.customs.inventorylinking.imports.services.ImportsConfigService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiSubscriptionFieldsConnector @Inject()(http: HttpClientV2,
                                               servicesConfig: ImportsConfigService,
                                               logger: ImportsLogger)
                                              (implicit ec: ExecutionContext) {

  def getSubscriptionFields[A](apiSubsKey: ApiSubscriptionKey)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[ApiSubscriptionFieldsResponse] = {
    val url = ApiSubscriptionFieldsPath.url(servicesConfig.importsConfig.apiSubscriptionFieldsBaseUrl, apiSubsKey)
    get(url)
  }

  private def get[A](url: String)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[ApiSubscriptionFieldsResponse] = {
    logger.debug(s"Getting fields id from api subscription fields service. url=$url")

    http.get(url"$url").execute[ApiSubscriptionFieldsResponse]
      .recoverWith {
        case httpError: HttpException =>
          Future.failed(new RuntimeException(httpError))

        case e: Throwable =>
          logger.error(s"Call to subscription information service failed. url=$url")
          Future.failed(e)
      }
  }
}
