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

package uk.gov.hmrc.customs.inventorylinking.imports.connectors

import java.net.URLEncoder
import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.mvc.AnyContent
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ApiSubscriptionFieldsResponse, ValidatedRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{ImportsConfigService, WSHttp}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApiSubscriptionFieldsConnector @Inject()(http: WSHttp,
                                               servicesConfig: ImportsConfigService,
                                               logger: ImportsLogger) {

  private val apiContextEncoded = URLEncoder.encode("customs/inventory-linking-imports", "UTF-8")
  private val version = "1.0"
  //TODO: pass in clientId explicitly
  def getClientSubscriptionId()(implicit rdWrapper: ValidatedRequest[AnyContent]): Future[UUID] = {
    val url = s"${servicesConfig.apiSubscriptionFieldsBaseUrl}/application/${rdWrapper.rdWrapper.clientId.getOrElse(throw new IllegalStateException("clientId not present"))}/context/$apiContextEncoded/version/$version"
    get(url).map(r => r.fieldsId)
  }

  private def get(url: String)(implicit rd: ValidatedRequest[AnyContent]): Future[ApiSubscriptionFieldsResponse] = {
    logger.debug(s"Getting fields id from api-subscription-fields service. url = $url")
    implicit val hc: HeaderCarrier = rd.rdWrapper.headerCarrier

    http.GET[ApiSubscriptionFieldsResponse](url)
      .recoverWith {
        case httpError: HttpException => Future.failed(new RuntimeException(httpError))
      }
      .recoverWith {
        case e: Throwable =>
          logger.error(s"Call to get api subscription fields failed. url = $url")
          Future.failed(e)
      }
  }
}
