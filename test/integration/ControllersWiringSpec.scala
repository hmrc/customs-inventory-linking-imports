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

package integration

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders.{GoodsArrivalPayloadValidationAction, ValidateMovementPayloadValidationAction}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.{GoodsArrivalController, ValidateMovementController}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.services.{GoodsArrivalXmlValidationService, ValidateMovementXmlValidationService}

class ControllersWiringSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar {

  private implicit val ec = Helpers.stubControllerComponents().executionContext
  private lazy val mockGoodsArrivalXmlValidationService = mock[GoodsArrivalXmlValidationService]
  private lazy val mockValidateMovementXmlValidationService = mock[ValidateMovementXmlValidationService]
  private lazy val mockImportLogger = mock[ImportsLogger]
  private lazy val goodsArrivalController = app.injector.instanceOf[GoodsArrivalController]
  private lazy val validateMovementController = app.injector.instanceOf[ValidateMovementController]

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder().configure(Map(
      "metrics.enabled" -> false
    )).build()

  "The correct XmlValidationAction"  should {
    "be wired into goodsArrivalController" in {
      val action = goodsArrivalController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new GoodsArrivalPayloadValidationAction(mockGoodsArrivalXmlValidationService, mockImportLogger).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.goodsarrival"
    }

    "be wired into validateMovementController" in {
      val action = validateMovementController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new ValidateMovementPayloadValidationAction(mockValidateMovementXmlValidationService, mockImportLogger).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.validatemovement"
    }
  }

}
