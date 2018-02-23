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

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.inventorylinking.imports.services.WSHttp
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ValidateMovementConnector @Inject()(wsHttp: WSHttp) {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  def post(request: OutgoingRequest): Future[HttpResponse] = {

    wsHttp.POSTString(request.url, request.body.toString, request.headers).
      recoverWith {
        case httpError: HttpException => Future.failed(new RuntimeException(httpError))
      }
  }
}
