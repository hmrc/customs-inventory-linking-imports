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

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.InventoryLinkingImportsConnector
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ValidateMovementController
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.xml.NodeSeq

class ValidateMovementControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  private val connector = mock[InventoryLinkingImportsConnector]

  "POST valid declaration" when {
    "MDG backend service connector returns ACCEPTED" should {
      "return 202 ACCEPTED" in {
        val controller = new ValidateMovementController(connector)
        val payload = <payload></payload>

        when(connector.sendValidateMovementMessage(meq(payload))(any[HeaderCarrier])).
          thenReturn(Future.successful(HttpResponse(ACCEPTED)))

        val request = FakeRequest().withXmlBody(payload)
        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe ACCEPTED
      }
    }

    "MDG backend service connector returns failure" should {
      "return 500 Internal Server Error" in {
        val controller = new ValidateMovementController(connector)

        when(connector.sendValidateMovementMessage(any[NodeSeq])(any[HeaderCarrier])).
          thenReturn(Future.failed(new UnsupportedOperationException))

        val result = await(controller.postMessage("id").apply(FakeRequest()))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
