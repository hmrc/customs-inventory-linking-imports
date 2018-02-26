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

import com.github.tomakehurst.wiremock.client.WireMock.{status => _, _}
import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import unit.util.XMLTestData.ValidInventoryLinkingMovementRequestXML
import util.{ExternalServicesConfig, TestData, WireMockRunner}

import scala.concurrent.Future
import scala.xml.{Utility, XML}

class ImportsControllerSpec extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite
  with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with WireMockRunner with TableDrivenPropertyChecks {

  private val importMovementUrl = "/inventorylinkingimports/validatemovementresponse"
  private val id = "id"

  private val validateMovementRequest = FakeRequest("POST", s"/$id/movement-validation")
    .withXmlBody(ValidInventoryLinkingMovementRequestXML)
    .withHeaders(TestData.Headers.validHeaders: _*)

  private val goodsArrivalRequest = FakeRequest("POST", s"/$id/arrival-notification")
    .withXmlBody(ValidInventoryLinkingMovementRequestXML)
    .withHeaders(TestData.Headers.validHeaders: _*)

  private val internalServerError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>INTERNAL_SERVER_ERROR</code>
      |  <message>Internal server error</message>
      |</errorResponse>
    """.stripMargin

  private val validMessageMatcher = post(urlMatching(importMovementUrl)).
    withHeader(ACCEPT, equalTo(MimeTypes.XML)).
    withHeader(CONTENT_TYPE, equalTo(s"${MimeTypes.XML}; charset=UTF-8")).
    withHeader(DATE, notMatching("")).
    withHeader("X-Correlation-ID", notMatching("")).
    withHeader(X_FORWARDED_HOST, equalTo("MDTP")).
    withHeader(AUTHORIZATION, equalTo(s"Bearer ${ExternalServicesConfig.AuthToken}"))

  override def fakeApplication(): Application  = new GuiceApplicationBuilder().configure(Map(
    "microservice.services.validatemovement.host" -> ExternalServicesConfig.Host,
    "microservice.services.validatemovement.port" -> ExternalServicesConfig.Port,
    "microservice.services.validatemovement.context" -> importMovementUrl,
    "microservice.services.validatemovement.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.goodsarrival.host" -> ExternalServicesConfig.Host,
    "microservice.services.goodsarrival.port" -> ExternalServicesConfig.Port,
    "microservice.services.goodsarrival.context" -> importMovementUrl,
    "microservice.services.goodsarrival.bearer-token" -> ExternalServicesConfig.AuthToken
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

  private val controllers = Table(("controller name", "controller post"),
    ("Goods Arrival", goodsArrivalRequest),
    ("Validate Movement", validateMovementRequest)
  )

  forAll(controllers) { case (controllerName, controller) =>

    feature(s"CSP Submits ($controllerName) Message") {
      info("As a CSP")
      info(s"I want to submit an import inventory linking $controllerName message")
      info("So that my consignment can continue on its journey")

      scenario(s"A valid $controllerName message submitted and successfully forwarded to the backend") {
        Given("a CSP is authorised to use the API endpoint")

        And("the Back End Service will return a successful response")

        stubFor(validMessageMatcher willReturn aResponse().withStatus(ACCEPTED))

        When(s"a valid $controllerName message is submitted with valid headers")
        val result: Future[Result] = route(app, controller).get

        And("an Accepted (202) response is returned")
        status(result) shouldBe ACCEPTED
        header("X-Conversation-ID", result).get shouldNot be("")
      }

      scenario(s"A valid $controllerName submitted and the Back End service fails") {
        Given("a CSP is authorised to use the API endpoint")

        And("the Back End Service will return an error response")
        stubFor(
          post(urlMatching(importMovementUrl)).
            willReturn(aResponse().withStatus(NOT_FOUND)))


        When(s"a valid $controllerName message request is submitted")
        val result = route(app, controller).get

        Then("an 500 Internal Server Error response is returned")
        status(result) shouldBe INTERNAL_SERVER_ERROR
        stringToXml(contentAsString(result)) shouldEqual stringToXml(internalServerError)
        header("X-Conversation-ID", result).get shouldNot be("")
      }
    }
  }

  private def stringToXml(str: String) = {
    Utility.trim(XML.loadString(str))
  }
}
