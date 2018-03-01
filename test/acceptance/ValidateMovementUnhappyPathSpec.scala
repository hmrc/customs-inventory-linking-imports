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
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Headers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import unit.util.XMLTestData.InvalidInventoryLinkingMovementRequestXMLWithMultipleErrors
import util.{ExternalServicesConfig, WireMockRunner}

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.{Node, NodeSeq, Utility, XML}

class ValidateMovementUnhappyPathSpec extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite
  with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with WireMockRunner with OptionValues {

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

  val id = "id"
  val payload = <import>payload</import>
  val cspBearerToken = "CSP-Bearer-Token"
  val X_CONVERSATION_ID_NAME = "X-Conversation-ID"
  private val endpoint = s"/$id/movement-validation"

  private val BadRequestError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Bad Request</message>
      |  <errors>
      |    <error>
      |      <code>xml_validation_error</code>
      |      <message>cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'InventoryLinkingImportsValidateMovementResponse'.</message>
      |    </error>
      |    <error>
      |      <code>xml_validation_error</code>
      |      <message>cvc-pattern-valid: Value 'abc_123' is not facet-valid with respect to pattern '([a-zA-Z0-9])*' for type 'entryNumber'.</message>
      |    </error>
      |    <error>
      |      <code>xml_validation_error</code>
      |      <message>cvc-type.3.1.3: The value 'abc_123' of element 'entryNumber' is not valid.</message>
      |    </error>
      |    <error>
      |      <code>xml_validation_error</code>
      |      <message>cvc-datatype-valid.1.2.1: 'A' is not a valid value for 'integer'.</message>
      |    </error>
      |    <error>
      |      <code>xml_validation_error</code>
      |      <message>cvc-type.3.1.3: The value 'A' of element 'entryVersionNumber' is not valid.</message>
      |    </error>
      |  </errors>
      |</errorResponse>""".stripMargin


  private val malformedXmlBadRequest =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Bad Request</message>
      |  <errors>
      |    <error>
      |      <code>xml_validation_error</code>
      |      <message>Premature end of file.</message>
      |    </error>
      |  </errors>
      |</errorResponse>""".stripMargin

  private val nonXmlPayloadBadRequest =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Bad Request</message>
      |  <errors>
      |    <error>
      |      <code>xml_validation_error</code>
      |      <message>Premature end of file.</message>
      |    </error>
      |  </errors>
      |</errorResponse>""".stripMargin

  feature("The API handles errors as expected") {

    scenario("Response status 400 when user submits an XML payload that does not adhere to schema") {

      Given("an authorised CSP wants to submit a customs UKCIRM message with an invalid XML payload")
      When("a POST request with data is sent to the API")

      val result = postValidMovementMessage(InvalidInventoryLinkingMovementRequestXMLWithMultipleErrors)

      Then("a response with a 400 status is returned by the API")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"invalid xml\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestError)
    }

    scenario("Response status 400 when user submits a malformed XML payload") {

      Given("an authorised CSP wants to submit a customs UKCIRM message with a malformed XML payload")
      When("a POST request with data is sent to the API")
      val result = postValidMovementMessage("<xm> malformed xml <xm>")

      Then("a response with a 400 status is received")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"malformed xml body\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(malformedXmlBadRequest)
    }

    scenario("Response status 400 when user submits a non-xml payload") {

      Given("an authorised CSP wants to submit a customs UKCIRM message with a non-XML payload")
      When("a POST request with data is sent to the API")
      val result = postValidMovementMessage("""  {"valid": "json payload" }  """)

      Then("a response with a 400 status is received")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"malformed xml body\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(nonXmlPayloadBadRequest)
    }
  }

  protected def string2xml(s: String): Node = {
    val xml = try {
      XML.loadString(s)
    } catch {
      case NonFatal(thr) => fail("Not an xml: " + s, thr)
    }
    Utility.trim(xml)
  }

  private def postValidMovementMessage(payload: String) = {
    val request = FakeRequest("POST", "/movement-validation").withBody(payload)
    route(app, request)
  }

  private def postValidMovementMessage(payload: NodeSeq) = {
    val request = FakeRequest("POST", "/movement-validation").withXmlBody(payload)
    route(app, request)
  }
}
