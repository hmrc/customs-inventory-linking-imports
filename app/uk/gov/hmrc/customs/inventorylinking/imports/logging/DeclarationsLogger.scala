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

package uk.gov.hmrc.customs.inventorylinking.imports.logging

import com.google.inject.Inject
import javax.inject.Singleton
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.logging.LoggingHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.Ids
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class DeclarationsLogger @Inject()(logger: CdsLogger) {

  val ids: Ids = Ids.empty()

  def debug(s: => String)(implicit hc: HeaderCarrier): Unit = logger.debug(formatDebug(s, ids))
  def info(s: => String)(implicit hc: HeaderCarrier): Unit = logger.info(formatInfo(s, Some(ids)))
  def warn(s: => String)(implicit hc: HeaderCarrier): Unit = logger.warn(formatWarn(s, Some(ids)))
  def error(s: => String)(implicit hc: HeaderCarrier): Unit = logger.error(formatError(s, Some(ids)))
  def errorWithoutHeaderCarrier(s: => String): Unit = logger.error(s)

}
