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
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsMessageType, ValidateMovement}
import util.TestData._
import util.XMLTestData.{InvalidInventoryLinkingGoodsArrivalRequestXML, InvalidInventoryLinkingMovementRequestXML}
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, InventoryLinkingImportsService}

class ImportsServiceUnhappyPathSpec extends AcceptanceTestSpec
  with Matchers
  with OptionValues
  with TableDrivenPropertyChecks
  with InventoryLinkingImportsService
  with ApiSubscriptionFieldsService
  with AuthService {

  private case class InvalidRequest(
    invalidRequest: FakeRequest[AnyContentAsXml],
    invalidRequestWithMalformedXml: FakeRequest[String],
    invalidRequestWithNonXml: FakeRequest[String]
  )
  private case class InvalidRequests(goodsArrival: InvalidRequest, validateMovement: InvalidRequest)

  private val invalidRequests =
    InvalidRequests(
      goodsArrival = InvalidRequest(
        invalidRequest = ValidGoodsArrivalRequest.withXmlBody(InvalidInventoryLinkingGoodsArrivalRequestXML),
        invalidRequestWithMalformedXml = ValidGoodsArrivalRequest.withBody("<xm> malformed xml <xm>"),
        invalidRequestWithNonXml = ValidGoodsArrivalRequest.withBody("""  {"valid": "json payload" }  """)
      ),
      validateMovement = InvalidRequest(
        invalidRequest = ValidValidateMovementRequest.withXmlBody(InvalidInventoryLinkingMovementRequestXML),
        invalidRequestWithMalformedXml = ValidValidateMovementRequest.withBody("<xm> malformed xml <xm>"),
        invalidRequestWithNonXml = ValidValidateMovementRequest.withBody("""  {"valid": "json payload" }  """)
      )
    )

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  private def badRequestError(messageType: ImportsMessageType) =
    s"""<?xml version="1.0" encoding="UTF-8"?>
       |<errorResponse>
       |    <code>BAD_REQUEST</code>
       |    <message>Bad Request</message>
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

  private val malformedXmlError =
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

  private val nonXmlPayloadError =
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

  private val unAuthorisedError =
    """<errorResponse>
      |    <code>UNAUTHORIZED</code>
      |    <message>Unauthorised request</message>
      |</errorResponse>""".stripMargin

  private val xmlRequestsByMessageType = Table(
      ("Message Type Description",
        "messageType",
        "Valid Request",
        "Invalid Request",
        "Invalid Request With Malformed Xml",
        "non XML"),
      ("Goods Arrival",
        GoodsArrival,
        ValidGoodsArrivalRequest,
        invalidRequests.goodsArrival.invalidRequest,
        invalidRequests.goodsArrival.invalidRequestWithMalformedXml,
        invalidRequests.goodsArrival.invalidRequestWithNonXml),
      ("Validate Movement",
        ValidateMovement,
        ValidValidateMovementRequest,
        invalidRequests.validateMovement.invalidRequest,
        invalidRequests.validateMovement.invalidRequestWithMalformedXml,
        invalidRequests.validateMovement.invalidRequestWithNonXml)
    )

  forAll(xmlRequestsByMessageType) { case (messageTypeDesc, messageType, validRequest, invalidRequest, invalidRequestWithMalformedXml, invalidRequestWithNonXml) =>

    feature(s"The $messageType endpoint handles errors as expected") {

      scenario(s"Response status 401 when non authorised user submits a valid $messageTypeDesc message") {

        Given(s"an un-authorised CSP wants to submit a customs $messageTypeDesc message with an invalid XML payload")
        authServiceUnAuthorisesScopeForCSP(messageType)
        When("a POST request with data is sent to the API")
        val result = route(app, validRequest.fromCsp)

        Then("a response with a 401 status is returned by the API")
        result shouldBe 'defined

        val resultFuture = result.get

        status(resultFuture) shouldBe UNAUTHORIZED
        headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

        And("the response body is a \"invalid xml\" XML")
        stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(unAuthorisedError)
      }

      scenario(s"Response status 400 when user submits a $messageTypeDesc message with an XML payload that does not adhere to schema") {

        Given(s"an authorised CSP wants to submit a customs $messageTypeDesc message with an invalid XML payload")
        authServiceAuthorisesCSP(messageType)
        When("a POST request with data is sent to the API")
        val result = route(app, invalidRequest.fromCsp)

        Then("a response with a 400 status is returned by the API")
        result shouldBe 'defined

        val resultFuture = result.get

        status(resultFuture) shouldBe BAD_REQUEST
        headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

        And("the response body is a \"Unauthorised request\" XML")
        stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(badRequestError(messageType))
      }

      scenario(s"Response status 400 when user submits a $messageTypeDesc message with a malformed XML payload") {

        Given(s"an authorised CSP wants to submit a customs $messageTypeDesc message with a malformed XML payload")
        authServiceAuthorisesCSP(messageType)
        When("a POST request with data is sent to the API")
        val result = route(app, invalidRequestWithMalformedXml.fromCsp)

        Then("a response with a 400 status is received")
        result shouldBe 'defined

        val resultFuture = result.get

        status(resultFuture) shouldBe BAD_REQUEST
        headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

        And("the response body is a \"malformed xml body\" XML")
        stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(malformedXmlError)
      }

      scenario(s"Response status 400 when user submits a $messageTypeDesc message with a non-xml payload") {

        Given(s"an authorised CSP wants to submit a customs $messageTypeDesc message with a non-XML payload")
        authServiceAuthorisesCSP(messageType)
        When("a POST request with data is sent to the API")
        val result = route(app, invalidRequestWithNonXml.fromCsp)

        Then("a response with a 400 status is received")
        result shouldBe 'defined

        val resultFuture = result.get

        status(resultFuture) shouldBe BAD_REQUEST
        headers(resultFuture).get(XConversationIdHeaderName) shouldBe 'defined

        And("the response body is a \"malformed xml body\" XML")
        stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(nonXmlPayloadError)
      }
    }
  }

}
