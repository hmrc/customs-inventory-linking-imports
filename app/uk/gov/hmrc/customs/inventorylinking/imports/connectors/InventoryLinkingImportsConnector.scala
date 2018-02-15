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

import javax.inject.Inject

import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.WSHttp
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

class InventoryLinkingImportsConnector @Inject()(wsHttp: WSHttp, configProvider: ServiceConfigProvider){

  def sendValidateMovementMessage(payload: NodeSeq)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val config = configProvider.getConfig("inventory-linking-imports")
    wsHttp.POSTString(config.url, payload.toString(), Seq()).
      recoverWith {
        case httpError: HttpException => Future.failed(new RuntimeException(httpError))
      }
  }
}
