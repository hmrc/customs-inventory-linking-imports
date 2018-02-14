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

package unit.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import play.api.http.Status.ACCEPTED
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ValidateMovementController

class ValidateMovementControllerSpec extends UnitSpec with GuiceOneAppPerSuite {

  "POST valid declaration" when {
    "MDG backend service returns ACCEPTED" should {
      "return 202 ACCEPTED" in {
        val controller = new ValidateMovementController
        val result = await(controller.postMessage("id").apply(FakeRequest()))

        status(result) shouldBe ACCEPTED
      }
    }
  }
}
