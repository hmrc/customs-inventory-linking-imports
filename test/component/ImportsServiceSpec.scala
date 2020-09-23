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

import org.scalatest._
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import util.TestData._
import util.XMLTestData.{InvalidInventoryLinkingGoodsArrivalRequestXML, InvalidInventoryLinkingMovementRequestXML, validWrappedGoodsArrivalXml, validWrappedValidateMovementXml}
import util.externalservices.InventoryLinkingImportsExternalServicesConfig._
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, CustomsMetricsService, InventoryLinkingImportsService}

import scala.concurrent.Future

class ImportsServiceSpec extends ComponentTestSpec with Matchers with OptionValues with BeforeAndAfterAll
  with BeforeAndAfterEach
  with CustomsMetricsService
  with TableDrivenPropertyChecks
  with InventoryLinkingImportsService
  with ApiSubscriptionFieldsService
  with AuthService {

  private val internalServerError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>INTERNAL_SERVER_ERROR</code>
      |  <message>Internal server error</message>
      |</errorResponse>
    """.stripMargin

  private def badRequestError(messageType: ImportsMessageType) =
    s"""<?xml version="1.0" encoding="UTF-8"?>
       |<errorResponse>
       |    <code>BAD_REQUEST</code>
       |    <message>Payload is not valid according to schema</message>
       |    <errors>
       |        <error>
       |            <code>xml_validation_error</code>
       |            <message>cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element
       |                '${elementName(messageType)}'.
       |            </message>
       |        </error>
       |    </errors>
       |</errorResponse>
     """.stripMargin

  private val malformedXmlAndNonXmlPayloadError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Request body does not contain a well-formed XML document.</message>
      |</errorResponse>""".stripMargin

  private val unAuthorisedError =
    """<errorResponse>
      |    <code>UNAUTHORIZED</code>
      |    <message>Unauthorised request</message>
      |</errorResponse>""".stripMargin

  protected val ServiceUnavailableError: String =
    """<?xml version='1.0' encoding='UTF-8'?>
      |<errorResponse>
      |      <code>SERVER_ERROR</code>
      |      <message>The 'customs/inventory-linking-imports' API is currently unavailable</message>
      |</errorResponse>
    """.stripMargin

  private val apiSubscriptionKeyImports =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Finventory-linking-imports", version = VersionOne)

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature(s"CSP Submits Goods Arrival Message") {
    info("As a CSP")
    info(s"I want to submit an import inventory linking Goods Arrival message")
    info("So that my consignment can continue on its journey")

    scenario(s"A valid Goods Arrival message submitted and successfully forwarded to the backend") {
      Given("a CSP is authorised to use the API endpoint")
      val goodsArrival = new GoodsArrival()
      authServiceAuthorisesCSP(goodsArrival)

      And("the Back End Service will return a successful response")
      startApiSubscriptionFieldsService(apiSubscriptionKeyImports)
      setupBackendServiceToReturn(GoodsArrivalConnectorContext, ACCEPTED)

      When(s"a valid Goods Arrival message is submitted with valid headers")
      val result: Future[Result] = route(app, ValidGoodsArrivalRequest.fromCsp).get

      And("an Accepted (202) response is returned")
      status(result) shouldBe ACCEPTED
      header(XConversationIdHeaderName, result).get shouldNot be("")

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp(goodsArrival.enrolment))

      And("the payload is correct")
      verifyImportsConnectorServiceWasCalledWith(GoodsArrivalConnectorContext, validWrappedGoodsArrivalXml.toString())

      And("Metrics logging call was made")
      eventually(verifyCustomsMetricsServiceWasCalled())
    }

    scenario(s"A valid Goods Arrival submitted and the Back End service fails") {
      Given("a CSP is authorised to use the API endpoint")
      authServiceAuthorisesCSP(new GoodsArrival())

      And("the Back End Service will return an error response")
      startApiSubscriptionFieldsService(apiSubscriptionKeyImports)
      setupBackendServiceToReturn(GoodsArrivalConnectorContext, NOT_FOUND)

      When(s"a valid Goods Arrival message request is submitted")
      val result = route(app, ValidGoodsArrivalRequest.fromCsp).get

      Then("an 500 Internal Server Error response is returned")
      status(result) shouldBe INTERNAL_SERVER_ERROR
      stringToXml(contentAsString(result)) shouldEqual stringToXml(internalServerError)
      header(XConversationIdHeaderName, result).get shouldNot be("")
    }
  }

  feature(s"The Goods Arrival endpoint handles errors as expected") {

    scenario(s"Response status 401 when non authorised user submits a valid Goods Arrival message") {

      Given(s"an un-authorised CSP wants to submit a customs Goods Arrival message with an invalid XML payload")
      authServiceUnAuthorisesScopeForCSP(new GoodsArrival())
      When("a POST request with data is sent to the API")
      val result = route(app, ValidGoodsArrivalRequest.fromCsp)

      Then("a response with a 401 status is returned by the API")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe UNAUTHORIZED
      headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

      And("the response body is a \"invalid xml\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(unAuthorisedError)
    }

    scenario(s"Response status 400 when user submits a Goods Arrival message with an XML payload that does not adhere to schema") {

      Given(s"an authorised CSP wants to submit a customs Goods Arrival message with an invalid XML payload")
      authServiceAuthorisesCSP(new GoodsArrival())
      When("a POST request with data is sent to the API")
      val result = route(app, ValidGoodsArrivalRequest.withXmlBody(InvalidInventoryLinkingGoodsArrivalRequestXML).fromCsp)

      Then("a response with a 400 status is returned by the API")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

      And("the response body is a \"Bad request\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(badRequestError(new GoodsArrival()))
    }

    scenario(s"Response status 400 when user submits a Goods Arrival message with a malformed XML payload") {

      Given(s"an authorised CSP wants to submit a customs Goods Arrival message with a malformed XML payload")
      authServiceAuthorisesCSP(new GoodsArrival())
      When("a POST request with data is sent to the API")
      val result = route(app, ValidGoodsArrivalRequest.withBody("<xm> malformed xml <xm>").fromCsp)

      Then("a response with a 400 status is received")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

      And("the response body is a \"malformed xml body\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(malformedXmlAndNonXmlPayloadError)
    }

    scenario(s"Response status 400 when user submits a Goods Arrival message with a non-xml payload") {

      Given(s"an authorised CSP wants to submit a customs Goods Arrival message with a non-XML payload")
      authServiceAuthorisesCSP(new GoodsArrival())
      When("a POST request with data is sent to the API")
      val result = route(app, ValidGoodsArrivalRequest.withBody("""  {"valid": "json payload" }  """).fromCsp)

      Then("a response with a 400 status is received")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

      And("the response body is a \"malformed xml body\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(malformedXmlAndNonXmlPayloadError)
    }
  }

  feature(s"CSP Submits Validate Movement Message") {
    info("As a CSP")
    info(s"I want to submit an import inventory linking Validate Movement message")
    info("So that my consignment can continue on its journey")

    scenario(s"A valid Validate Movement message submitted and successfully forwarded to the backend") {
      Given("a CSP is authorised to use the API endpoint")
      val validateMovement = new ValidateMovement()
      authServiceAuthorisesCSP(validateMovement)

      And("the Back End Service will return a successful response")
      startApiSubscriptionFieldsService(apiSubscriptionKeyImports)
      setupBackendServiceToReturn(ValidateMovementConnectorContext, ACCEPTED)

      When(s"a valid Validate Movement message is submitted with valid headers")
      val result: Future[Result] = route(app, ValidValidateMovementRequest.fromCsp).get

      And("an Accepted (202) response is returned")
      status(result) shouldBe ACCEPTED
      header(XConversationIdHeaderName, result).get shouldNot be("")

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp(validateMovement.enrolment))

      And("the payload is correct")
      verifyImportsConnectorServiceWasCalledWith(ValidateMovementConnectorContext, validWrappedValidateMovementXml.toString())

      And("Metrics logging call was made")
      eventually(verifyCustomsMetricsServiceWasCalled())
    }

    scenario(s"A valid Validate Movement submitted and the Back End service fails") {
      Given("a CSP is authorised to use the API endpoint")
      authServiceAuthorisesCSP(new ValidateMovement())

      And("the Back End Service will return an error response")
      startApiSubscriptionFieldsService(apiSubscriptionKeyImports)
      setupBackendServiceToReturn(ValidateMovementConnectorContext, NOT_FOUND)

      When(s"a valid Validate Movement message request is submitted")
      val result = route(app, ValidValidateMovementRequest.fromCsp).get

      Then("an 500 Internal Server Error response is returned")
      status(result) shouldBe INTERNAL_SERVER_ERROR
      stringToXml(contentAsString(result)) shouldEqual stringToXml(internalServerError)
      header(XConversationIdHeaderName, result).get shouldNot be("")
    }
  }

  feature(s"The Validate Movement endpoint handles errors as expected") {

    scenario(s"Response status 401 when non authorised user submits a valid Validate Movement message") {

      Given(s"an un-authorised CSP wants to submit a customs Validate Movement message with an invalid XML payload")
      authServiceUnAuthorisesScopeForCSP(new ValidateMovement())
      When("a POST request with data is sent to the API")
      val result = route(app, ValidValidateMovementRequest.fromCsp)

      Then("a response with a 401 status is returned by the API")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe UNAUTHORIZED
      headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

      And("the response body is a \"invalid xml\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(unAuthorisedError)
    }

    scenario(s"Response status 400 when user submits a Validate Movement message with an XML payload that does not adhere to schema") {

      Given(s"an authorised CSP wants to submit a customs Validate Movement message with an invalid XML payload")
      authServiceAuthorisesCSP(new ValidateMovement())
      When("a POST request with data is sent to the API")
      val result = route(app, ValidValidateMovementRequest.withXmlBody(InvalidInventoryLinkingMovementRequestXML).fromCsp)

      Then("a response with a 400 status is returned by the API")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

      And("the response body is a \"Bad request\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(badRequestError(new ValidateMovement()))
    }

    scenario(s"Response status 400 when user submits a Validate Movement message with a malformed XML payload") {

      Given(s"an authorised CSP wants to submit a customs Validate Movement message with a malformed XML payload")
      authServiceAuthorisesCSP(new ValidateMovement())
      When("a POST request with data is sent to the API")
      val result = route(app, ValidValidateMovementRequest.withBody("<xm> malformed xml <xm>").fromCsp)

      Then("a response with a 400 status is received")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

      And("the response body is a \"malformed xml body\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(malformedXmlAndNonXmlPayloadError)
    }

    scenario(s"Response status 400 when user submits a Validate Movement message with a non-xml payload") {

      Given(s"an authorised CSP wants to submit a customs Validate Movement message with a non-XML payload")
      authServiceAuthorisesCSP(new ValidateMovement())
      When("a POST request with data is sent to the API")
      val result = route(app, ValidValidateMovementRequest.withBody("""  {"valid": "json payload" }  """).fromCsp)

      Then("a response with a 400 status is received")
      result shouldBe 'defined

      val resultFuture = result.get

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

      And("the response body is a \"malformed xml body\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(malformedXmlAndNonXmlPayloadError)
    }
  }

  feature("Customs Inventory Linking Imports API returns unavailable when a version is shuttered") {
    scenario("An authorised CSP fails to submit a customs inventory linking imports request to a shuttered version") {
      Given("A CSP wants to submit a customs inventory linking imports request to a shuttered version")

      authServiceAuthorisesCSP(new ValidateMovement())

      val configMap: Map[String, Any] = Map("shutter.v1" -> "true", "metrics.jvm" -> false)
      implicit lazy val app: Application = new GuiceApplicationBuilder().configure(configMap).build()

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app, ValidValidateMovementRequest.withBody("<xm> xml </xm>").fromCsp)

      Then("a response with a 503 (SERVICE_UNAVAILABLE) status is received")
      val resultFuture = result.get

      status(resultFuture) shouldBe SERVICE_UNAVAILABLE

      And("the response body is")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(ServiceUnavailableError)

    }
  }

}
