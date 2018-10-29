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
import scalaz.ValidationNel
import scalaz.syntax.apply._
import scalaz.syntax.traverse._
import uk.gov.hmrc.customs.api.common.config.ConfigValidationNelAdaptor
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ImportsCircuitBreakerConfig, ImportsConfig}

@Singleton
class ImportsConfigService @Inject() (configValidationNel: ConfigValidationNelAdaptor, logger: ImportsLogger) {

  private val root = configValidationNel.root
  private val whiteListedCspApplicationIds = root.stringSeq("api.access.version-1.0.whitelistedApplicationIds")
  private val apiSubscriptionFieldsService = configValidationNel.service("api-subscription-fields")
  private val numberOfCallsToTriggerStateChangeNel = root.int("circuitBreaker.numberOfCallsToTriggerStateChange")
  private val unavailablePeriodDurationInMillisNel = root.int("circuitBreaker.unavailablePeriodDurationInMillis")
  private val unstablePeriodDurationInMillisNel = root.int("circuitBreaker.unstablePeriodDurationInMillis")
  private val apiSubscriptionFieldsServiceUrlNel = apiSubscriptionFieldsService.serviceUrl

  private val validatedImportsConfig: ValidationNel[String, ImportsConfig] = (
    whiteListedCspApplicationIds |@| apiSubscriptionFieldsServiceUrlNel
  )(ImportsConfig.apply)

  private val validatedImportsCircuitBreakerConfig: ValidationNel[String, ImportsCircuitBreakerConfig] = (
    numberOfCallsToTriggerStateChangeNel |@| unavailablePeriodDurationInMillisNel |@| unstablePeriodDurationInMillisNel
    ) (ImportsCircuitBreakerConfig.apply)

  private val importsConfigHolder =
    (validatedImportsConfig |@| validatedImportsCircuitBreakerConfig) (ImportsConfigHolder.apply) fold(
      fail = { nel =>
        // error case exposes nel (a NotEmptyList)
        val errorMsg = nel.toList.mkString("\n", "\n", "")
        logger.errorWithoutRequestContext(errorMsg)
        throw new IllegalStateException(errorMsg)
      },
      succ = identity
    )

  val importsConfig: ImportsConfig = importsConfigHolder.importsConfig

  val importsCircuitBreakerConfig: ImportsCircuitBreakerConfig = importsConfigHolder.importsCircuitBreakerConfig

  private case class ImportsConfigHolder(importsConfig: ImportsConfig,
                                         importsCircuitBreakerConfig: ImportsCircuitBreakerConfig)

}
