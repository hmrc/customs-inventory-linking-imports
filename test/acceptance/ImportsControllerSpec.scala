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

  private val validateMovementUrl = "/inventorylinkingimports/validatemovementresponse"
  private val goodsArrivalUrl = "/inventorylinkingimports/goodsarrivalnotification"
  private val id = "id"

  private val validateMovementRequest = FakeRequest("POST", "/movement-validation")
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

  private def validMessageMatcher(url: String) = post(urlMatching(url)).
    withHeader(ACCEPT, equalTo(MimeTypes.XML)).
    withHeader(CONTENT_TYPE, equalTo(s"${MimeTypes.XML}; charset=UTF-8")).
    withHeader(DATE, notMatching("")).
    withHeader("X-Correlation-ID", notMatching("")).
    withHeader(X_FORWARDED_HOST, equalTo("MDTP")).
    withHeader(AUTHORIZATION, equalTo(s"Bearer ${ExternalServicesConfig.AuthToken}"))

  override def fakeApplication(): Application  = new GuiceApplicationBuilder().configure(Map(
    "microservice.services.validatemovement.host" -> ExternalServicesConfig.Host,
    "microservice.services.validatemovement.port" -> ExternalServicesConfig.Port,
    "microservice.services.validatemovement.context" -> validateMovementUrl,
    "microservice.services.validatemovement.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.goodsarrival.host" -> ExternalServicesConfig.Host,
    "microservice.services.goodsarrival.port" -> ExternalServicesConfig.Port,
    "microservice.services.goodsarrival.context" -> goodsArrivalUrl,
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

  private val controllers = Table(("Message Type", "request", "url"),
    ("Goods Arrival", goodsArrivalRequest, goodsArrivalUrl),
    ("Validate Movement", validateMovementRequest, validateMovementUrl)
  )

  forAll(controllers) { case (messageType, request, url) =>

    feature(s"CSP Submits ($messageType) Message") {
      info("As a CSP")
      info(s"I want to submit an import inventory linking $messageType message")
      info("So that my consignment can continue on its journey")

      scenario(s"A valid $messageType message submitted and successfully forwarded to the backend") {
        Given("a CSP is authorised to use the API endpoint")

        And("the Back End Service will return a successful response")

        stubFor(validMessageMatcher(url) willReturn aResponse().withStatus(ACCEPTED))

        When(s"a valid $messageType message is submitted with valid headers")
        val result: Future[Result] = route(app, request).get

        And("an Accepted (202) response is returned")
        status(result) shouldBe ACCEPTED
        header("X-Conversation-ID", result).get shouldNot be("")
      }

      scenario(s"A valid $messageType submitted and the Back End service fails") {
        Given("a CSP is authorised to use the API endpoint")

        And("the Back End Service will return an error response")
        stubFor(
          post(urlMatching(url)).
            willReturn(aResponse().withStatus(NOT_FOUND)))


        When(s"a valid $messageType message request is submitted")
        val result = route(app, request).get

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
