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

import com.github.tomakehurst.wiremock.client.WireMock.{status => _}
import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.test.Helpers._
import util.TestData._
import util.externalservices.InventoryLinkingImportsExternalServicesConfig._
import util.externalservices.InventoryLinkingImportsService

class ImportsServiceUnhappyPathSpec extends AcceptanceTestSpec with Matchers with OptionValues with BeforeAndAfterAll
  with BeforeAndAfterEach with TableDrivenPropertyChecks with InventoryLinkingImportsService {

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  private val BadRequestError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      | <code>BAD_REQUEST</code>
      | <message>Bad Request</message>
      | <errors>
      |   <error>
      |     <code>xml_validation_error</code>
      |     <message>cvc-elt.1.a: Cannot find the declaration of element 'inventoryLinkingImportsGoodsArrival'.</message>
      |   </error>
      | </errors>
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

  private val controllers = Table(("Message Type", "request", "url"),
    ("Goods Arrival", ValidGoodsArrivalRequest, goodsArrivalConnectorContext),
    ("Validate Movement", ValidValidateMovementRequest, validateMovementConnectorContext)
  )

  forAll(controllers) { case (messageType, request, url) =>

    feature(s"The $messageType endpoint handles errors as expected") {

      scenario(s"Response status 400 when user submits a $messageType message with an XML payload that does not adhere to schema") {

        Given(s"an authorised CSP wants to submit a customs $messageType message with an invalid XML payload")
        When("a POST request with data is sent to the API")
        val result = route(app, InvalidRequest)

        Then("a response with a 400 status is returned by the API")
        result shouldBe 'defined

        val resultFuture = result.get

        status(resultFuture) shouldBe BAD_REQUEST
        headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

        And("the response body is a \"invalid xml\" XML")
        stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(BadRequestError)
      }

      scenario(s"Response status 400 when user submits a $messageType message with a malformed XML payload") {

        Given(s"an authorised CSP wants to submit a customs $messageType message with a malformed XML payload")
        When("a POST request with data is sent to the API")
        val result = route(app, InvalidRequestWithMalformedXml)

        Then("a response with a 400 status is received")
        result shouldBe 'defined

        val resultFuture = result.get

        status(resultFuture) shouldBe BAD_REQUEST
        headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

        And("the response body is a \"malformed xml body\" XML")
        stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(malformedXmlBadRequest)
      }

      scenario(s"Response status 400 when user submits a $messageType message with a non-xml payload") {

        Given(s"an authorised CSP wants to submit a customs $messageType message with a non-XML payload")
        When("a POST request with data is sent to the API")
        val result = route(app, InvalidRequestWithNonXml)

        Then("a response with a 400 status is received")
        result shouldBe 'defined

        val resultFuture = result.get

        status(resultFuture) shouldBe BAD_REQUEST
        headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

        And("the response body is a \"malformed xml body\" XML")
        stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(nonXmlPayloadBadRequest)
      }
    }
  }

}
