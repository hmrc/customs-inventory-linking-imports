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

package uk.gov.hmrc.customs.inventorylinking.imports.request

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}

import scala.concurrent.Future

class RequestInfoGenerator {
  def newRequestInfo: Future[RequestInfo] = {
    val conversationId = UUID.randomUUID()
    val correlationId = UUID.randomUUID()
    Future.successful(RequestInfo(conversationId, correlationId, DateTime.now(DateTimeZone.UTC)))
  }
}
