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

import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ValidateMovementController
import uk.gov.hmrc.customs.inventorylinking.imports.mdg.{Connector, MdgRequest, MdgRequestBuilder}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.xml.Elem

class ValidateMovementControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  trait Setup {
    private val serviceConfigProvider: ServiceConfigProvider = mock[ServiceConfigProvider]
    private val body: Elem = <payload>payload</payload>
    private val serviceConfig: ServiceConfig = ServiceConfig("/url/", Some("Bearer"), "environment")
    private val requestBuilder: MdgRequestBuilder = mock[MdgRequestBuilder]

    val connector: Connector = mock[Connector]
    val mdgRequest = MdgRequest(serviceConfig, body)
    val request: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(body)
    val controller = new ValidateMovementController(connector, serviceConfigProvider, requestBuilder)

    when(serviceConfigProvider.getConfig("mdg-imports")).thenReturn(serviceConfig)

    when(requestBuilder.buildRequest(serviceConfig, body)).thenReturn(mdgRequest)

    def stubConnectorReturnsResponseForPostedMdgRequest(response: Future[HttpResponse]): Unit ={
      when(connector.postRequestToMdg(mdgRequest)).
        thenReturn(Future.successful(response))
    }
  }

  "POST valid declaration" when {
    "MDG backend service connector returns ACCEPTED" should {
      "return 202 ACCEPTED" in new Setup {
        stubConnectorReturnsResponseForPostedMdgRequest(HttpResponse(ACCEPTED))

        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe ACCEPTED
      }
    }

    "MDG backend service connector returns failure" should {
      "return 500 Internal Server Error" in  new Setup {
        stubConnectorReturnsResponseForPostedMdgRequest(Future.failed(new UnsupportedOperationException))

        val result = await(controller.postMessage("id").apply(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
