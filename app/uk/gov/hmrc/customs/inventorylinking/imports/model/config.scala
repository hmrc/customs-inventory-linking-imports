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

import uk.gov.hmrc.auth.core.Enrolment

trait ImportsMessageType {
  def name: String
  def wrapperRootElementLabel: String
  def enrolment: Enrolment
}

case object GoodsArrival extends ImportsMessageType {
  val name = "goodsarrival"
  val wrapperRootElementLabel = "InventoryLinkingImportsInboundGoodsArrival"
  val enrolment = Enrolment("write:customs-il-imports-arrival-notifications")
}

case object ValidateMovement extends ImportsMessageType {
  val name = "validatemovement"
  val wrapperRootElementLabel = "InventoryLinkingImportsInboundValidateMovementResponse"
  val enrolment = Enrolment("write:customs-il-imports-movement-validation")
}

trait ImportsConfig {
  val whiteListedCspApplicationIds: Seq[String]
  val apiSubscriptionFieldsBaseUrl: String
}
