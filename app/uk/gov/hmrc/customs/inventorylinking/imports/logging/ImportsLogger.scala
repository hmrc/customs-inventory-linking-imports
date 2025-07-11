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

package uk.gov.hmrc.customs.inventorylinking.imports.logging

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.mvc.Request
import uk.gov.hmrc.customs.inventorylinking.imports.logging.LoggingHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.EntryNumber
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.HasConversationId

@Singleton
class ImportsLogger @Inject()(logger: CdsLogger) {

  def debug(s: => String)(implicit r: HasConversationId): Unit =
    logger.debug(formatDebug(s, r))

  def debug(s: => String, e: => Throwable)(implicit r: HasConversationId): Unit =
    logger.debug(formatDebug(s, r), e)

  //called once at the start of the request processing pipeline
  def debugFull(s: => String)(implicit r: HasConversationId & Request[?]): Unit =
    logger.debug(formatDebugFull(s, r))

  def info(s: => String)(implicit r: HasConversationId): Unit =
    logger.info(formatInfo(s, r))

  def infoEn(s: => String, en: Option[EntryNumber])(implicit r: HasConversationId): Unit =
    logger.info(formatInfoEn(s, r, en))

  def warn(s: => String)(implicit r: HasConversationId): Unit =
    logger.warn(formatWarn(s, r))

  def warn(s: => String, e: => Throwable)(implicit r: HasConversationId): Unit =
    logger.warn(formatWarn(s, r), e)

  def error(s: => String, e: => Throwable)(implicit r: HasConversationId): Unit =
    logger.error(formatError(s, r), e)

  def error(s: => String)(implicit r: HasConversationId): Unit =
    logger.error(formatError(s, r))

  def errorWithoutRequestContext(s: => String): Unit =
    logger.error(s)
}
