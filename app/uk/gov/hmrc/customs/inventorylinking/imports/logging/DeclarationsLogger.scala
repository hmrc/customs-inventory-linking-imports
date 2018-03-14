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

import javax.inject.Singleton

import com.google.inject.Inject
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.logging.LoggingHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestDataWrapper

@Singleton
class DeclarationsLogger @Inject()(logger: CdsLogger) {

  def debug(s: => String)(implicit rdWrapper: RequestDataWrapper): Unit = logger.debug(formatDebug(s, rdWrapper))
  def info(s: => String)(implicit rdWrapper: RequestDataWrapper): Unit = logger.info(formatInfo(s, rdWrapper))
  def warn(s: => String)(implicit rdWrapper: RequestDataWrapper): Unit = logger.warn(formatWarn(s, rdWrapper))
  def error(s: => String)(implicit rdWrapper: RequestDataWrapper): Unit = logger.error(formatError(s, rdWrapper))
}
