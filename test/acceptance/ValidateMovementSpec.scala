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

package acceptance

import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.{ExternalServicesConfig, WireMockRunner}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching, equalToXml}

class ValidateMovementSpec extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite
  with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with WireMockRunner {

  val mdgImportMovementUrl = "/InventoryLinking/ImportMovement"

  override def fakeApplication(): Application  = new GuiceApplicationBuilder().configure(Map(
    "microservice.services.inventory-linking-imports.host" -> ExternalServicesConfig.Host,
    "microservice.services.inventory-linking-imports.port" -> ExternalServicesConfig.Port,
    "microservice.services.inventory-linking-imports.context" -> mdgImportMovementUrl,
    "microservice.services.inventory-linking-imports.bearer-token" -> ExternalServicesConfig.AuthToken
  )).build()

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("CSP Submits Validate Movement Response (UKCIRM) Message") {
    info("As a CSP")
    info("I want to submit an import inventory linking UKCIRM message")
    info("So that my consignment can continue on its journey")

    scenario("A valid UKCIRM message submitted and successfully forwarded to the backend") {
      Given("a CSP is authorised to use the API endpoint")

      And("the Back End Service will return a successful response")
      val id = "id"
      val payload = "<import>payload</import>"

      stubFor(
        post(urlMatching(mdgImportMovementUrl)).
          withRequestBody(equalToXml(payload)).
          willReturn(aResponse().withStatus(ACCEPTED)))

      When("a valid UKCIRM message is submitted with valid headers")
      val request = FakeRequest("POST", s"/$id/movement-validation").withBody(payload)
      val result = route(app, request).get

      And("an Accepted (202) response is returned")
      status(result) shouldBe ACCEPTED
    }

  }
}
