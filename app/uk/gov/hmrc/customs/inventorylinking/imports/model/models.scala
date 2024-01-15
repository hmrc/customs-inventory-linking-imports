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

package uk.gov.hmrc.customs.inventorylinking.imports.model

import java.util.UUID

import play.api.libs.json.{JsString, Reads, Writes}

case class Eori(value: String) extends AnyVal

case class ClientId(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class ConversationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}
object ConversationId {
  implicit val writer: Writes[ConversationId] = Writes[ConversationId] { x => JsString(x.uuid.toString) }
  implicit val reader: Reads[ConversationId] = Reads.of[UUID].map(new ConversationId(_))
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
  val configPrefix: String
  override def toString: String = value
}
object VersionOne extends ApiVersion {
  override val value: String = "1.0"
  override val configPrefix: String = ""
}
object VersionTwo extends ApiVersion {
  override val value: String = "2.0"
  override val configPrefix: String = "v2."
}

case class ImportsConfig (
  apiSubscriptionFieldsBaseUrl: String,
  customsMetricsBaseUrl: String
)

case class ImportsShutterConfig(v1Shuttered: Option[Boolean],
                                v2Shuttered: Option[Boolean])

case class ImportsCircuitBreakerConfig(numberOfCallsToTriggerStateChange: Int,
                                       unavailablePeriodDurationInMillis: Int,
                                       unstablePeriodDurationInMillis: Int)

