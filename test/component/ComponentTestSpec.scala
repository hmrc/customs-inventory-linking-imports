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

package component

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import util.ExternalServicesConfig
import util.externalservices.InventoryLinkingImportsExternalServicesConfig

import scala.xml.{Node, Utility, XML}

trait ComponentTestSpec extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite
  with BeforeAndAfterAll with BeforeAndAfterEach {

  override def fakeApplication(): Application  = new GuiceApplicationBuilder().configure(Map(
    "microservice.services.auth.host" -> ExternalServicesConfig.Host,
    "microservice.services.auth.port" -> ExternalServicesConfig.Port,
    "microservice.services.api-subscription-fields.host" -> ExternalServicesConfig.Host,
    "microservice.services.api-subscription-fields.port" -> ExternalServicesConfig.Port,
    "microservice.services.api-subscription-fields.context" -> InventoryLinkingImportsExternalServicesConfig.ApiSubscriptionFieldsContext,
    "microservice.services.validatemovement.host" -> ExternalServicesConfig.Host,
    "microservice.services.validatemovement.port" -> ExternalServicesConfig.Port,
    "microservice.services.validatemovement.context" -> InventoryLinkingImportsExternalServicesConfig.ValidateMovementConnectorContext,
    "microservice.services.validatemovement.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.goodsarrival.host" -> ExternalServicesConfig.Host,
    "microservice.services.goodsarrival.port" -> ExternalServicesConfig.Port,
    "microservice.services.goodsarrival.context" -> InventoryLinkingImportsExternalServicesConfig.GoodsArrivalConnectorContext,
    "microservice.services.goodsarrival.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.customs-metrics.host" -> ExternalServicesConfig.Host,
    "microservice.services.customs-metrics.port" -> ExternalServicesConfig.Port,
    "microservice.services.customs-metrics.context" -> InventoryLinkingImportsExternalServicesConfig.CustomsMetricsContext
  )).build()

  def stringToXml(str: String): Node = {
    Utility.trim(XML.loadString(str))
  }
}
