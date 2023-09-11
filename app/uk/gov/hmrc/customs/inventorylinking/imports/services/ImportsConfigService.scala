/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.implicits._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.api.common.config.{ConfigValidatedNelAdaptor, CustomsValidatedNel}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ImportsCircuitBreakerConfig, ImportsConfig, ImportsShutterConfig}

@Singleton
class ImportsConfigService @Inject() (configValidatedNel: ConfigValidatedNelAdaptor, logger: ImportsLogger) {

  private val root = configValidatedNel.root
  private val apiSubscriptionFieldsService = configValidatedNel.service("api-subscription-fields")
  private val apiSubscriptionFieldsServiceUrlNel = apiSubscriptionFieldsService.serviceUrl
  private val customsMetricsService = configValidatedNel.service("customs-declarations-metrics")
  private val customsMetricsServiceUrlNel = customsMetricsService.serviceUrl
  private val numberOfCallsToTriggerStateChangeNel = root.int("circuitBreaker.numberOfCallsToTriggerStateChange")
  private val unavailablePeriodDurationInMillisNel = root.int("circuitBreaker.unavailablePeriodDurationInMillis")
  private val unstablePeriodDurationInMillisNel = root.int("circuitBreaker.unstablePeriodDurationInMillis")
  private val v1ShutteredNel = root.maybeBoolean("shutter.v1")
  private val v2ShutteredNel = root.maybeBoolean("shutter.v2")

  private val validatedImportsConfig: CustomsValidatedNel[ImportsConfig] = (
    apiSubscriptionFieldsServiceUrlNel, customsMetricsServiceUrlNel
  ) mapN  ImportsConfig.apply

  private val validatedImportsShutterConfig: CustomsValidatedNel[ImportsShutterConfig] = (
    v1ShutteredNel, v2ShutteredNel
  ) mapN ImportsShutterConfig

  private val validatedImportsCircuitBreakerConfig: CustomsValidatedNel[ImportsCircuitBreakerConfig] = (
    numberOfCallsToTriggerStateChangeNel, unavailablePeriodDurationInMillisNel, unstablePeriodDurationInMillisNel
    ) mapN ImportsCircuitBreakerConfig

  private val importsConfigHolder: ImportsConfigHolder =
    (validatedImportsConfig, validatedImportsShutterConfig, validatedImportsCircuitBreakerConfig) mapN ImportsConfigHolder fold(
      // error
      { nel =>
        // error case exposes nel (a NotEmptyList)
        val errorMsg = nel.toList.mkString("\n", "\n", "")
        logger.errorWithoutRequestContext(errorMsg)
        throw new IllegalStateException(errorMsg)
      },
      // success
      identity
    )

  val importsConfig: ImportsConfig = importsConfigHolder.importsConfig

  val importsShutterConfig: ImportsShutterConfig = importsConfigHolder.importsShutterConfig

  val importsCircuitBreakerConfig: ImportsCircuitBreakerConfig = importsConfigHolder.importsCircuitBreakerConfig

  private case class ImportsConfigHolder(importsConfig: ImportsConfig,
                                        importsShutterConfig: ImportsShutterConfig,
                                         importsCircuitBreakerConfig: ImportsCircuitBreakerConfig)

}
