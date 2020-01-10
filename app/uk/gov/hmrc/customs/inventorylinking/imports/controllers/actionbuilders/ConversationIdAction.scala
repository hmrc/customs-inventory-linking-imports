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

package uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders

import javax.inject.{Inject, Singleton}
import play.api.mvc.{ActionTransformer, Request}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.customs.inventorylinking.imports.services.UniqueIdsService

import scala.concurrent.{ExecutionContext, Future}

/** Action builder that transforms a `Request` to a `ConversationIdRequest`
  *
  */
@Singleton
class ConversationIdAction @Inject()(uniqueIdService: UniqueIdsService,
                                     logger: ImportsLogger)
                                    (implicit ec: ExecutionContext)
  extends ActionTransformer[Request, ConversationIdRequest] {

  protected def executionContext: ExecutionContext = ec

  override def transform[A](request: Request[A]): Future[ConversationIdRequest[A]] = {

    val r = ConversationIdRequest(uniqueIdService.conversation, request)
    logger.debugFull("In ConversationIdAction.")(r)

    Future.successful(r)
  }
}
