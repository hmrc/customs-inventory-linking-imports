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

package uk.gov.hmrc.customs.inventorylinking.imports.model

import java.util.UUID

case class Eori(value: String) extends AnyVal

case class ClientId(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class ConversationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

case class CorrelationIdHeader(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class SubmitterIdentifier(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class AuthenticatedEori(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class CorrelationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

case class BadgeIdentifier(value: String) extends AnyVal

case class FieldsId(value: String) extends AnyVal

sealed trait ApiVersion {
  val value: String
  override def toString: String = value
}
object VersionOne extends ApiVersion{
  override val value: String = "1.0"
}

case class ImportsConfig (
  whiteListedCspApplicationIds: Seq[String],
  apiSubscriptionFieldsBaseUrl: String,
  authenticatedEori: String
)

case class ImportsCircuitBreakerConfig(numberOfCallsToTriggerStateChange: Int,
                                       unavailablePeriodDurationInMillis: Int,
                                       unstablePeriodDurationInMillis: Int)

