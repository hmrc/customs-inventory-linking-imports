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

package uk.gov.hmrc.customs.inventorylinking.imports.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.api.common.config.ConfigValidationNelAdaptor
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ApiDefinitionConfig, ImportsConfig}

import scalaz.ValidationNel
import scalaz.syntax.apply._
import scalaz.syntax.traverse._

@Singleton
class ImportsConfigService @Inject() (configValidationNel: ConfigValidationNelAdaptor, logger: CdsLogger) extends ImportsConfig {
  private val root = configValidationNel.root

  private val validatedApiDefinitionConfig: ValidationNel[String, ApiDefinitionConfig] = (
    root.string("customs.definition.api-scope") |@|
    root.stringSeq("api.access.version-1.0.whitelistedApplicationIds")
  )(ApiDefinitionConfig.apply)

  val apiDefinitionConfig: ApiDefinitionConfig = validatedApiDefinitionConfig.fold({
    nel => // error case exposes nel (a NotEmptyList)
      val errorMsg = "\n" + nel.toList.mkString("\n")
      logger.error(errorMsg)
      throw new IllegalStateException(errorMsg)
  },
    config => config // success case exposes the value class
  )

}
