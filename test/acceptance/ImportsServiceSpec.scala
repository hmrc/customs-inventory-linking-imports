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
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ApiSubscriptionKey, GoodsArrival, ValidateMovement, VersionOne}
import util.TestData._
import util.externalservices.InventoryLinkingImportsExternalServicesConfig._
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, InventoryLinkingImportsService}

import scala.concurrent.Future

class ImportsServiceSpec extends AcceptanceTestSpec with Matchers with OptionValues with BeforeAndAfterAll
  with BeforeAndAfterEach
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

  private val controllers = Table(("Message Type Description", "Imports Message Type", "request", "url"),
    ("Goods Arrival", GoodsArrival, ValidGoodsArrivalRequest.fromCsp, GoodsArrivalConnectorContext),
    ("Validate Movement", ValidateMovement, ValidValidateMovementRequest.fromCsp, ValidateMovementConnectorContext)
  )

  forAll(controllers) { case (messageTypeDesc, messageType, request, url) =>

    feature(s"CSP Submits $messageTypeDesc Message") {
      info("As a CSP")
      info(s"I want to submit an import inventory linking $messageTypeDesc message")
      info("So that my consignment can continue on its journey")

      scenario(s"A valid $messageTypeDesc message submitted and successfully forwarded to the backend") {
        Given("a CSP is authorised to use the API endpoint")
        authServiceAuthorisesCSP(messageType)

        And("the Back End Service will return a successful response")
        startApiSubscriptionFieldsService(apiSubscriptionKeyImports)
        setupBackendServiceToReturn(url, ACCEPTED)

        When(s"a valid $messageTypeDesc message is submitted with valid headers")
        val result: Future[Result] = route(app, request).get

        And("an Accepted (202) response is returned")
        status(result) shouldBe ACCEPTED
        header(XConversationIdHeaderName, result).get shouldNot be("")
      }

      scenario(s"A valid $messageTypeDesc submitted and the Back End service fails") {
        Given("a CSP is authorised to use the API endpoint")
        authServiceAuthorisesCSP(messageType)

        And("the Back End Service will return an error response")
        startApiSubscriptionFieldsService(apiSubscriptionKeyImports)
        setupBackendServiceToReturn(url, NOT_FOUND)

        When(s"a valid $messageTypeDesc message request is submitted")
        val result = route(app, request).get

        Then("an 500 Internal Server Error response is returned")
        status(result) shouldBe INTERNAL_SERVER_ERROR
        stringToXml(contentAsString(result)) shouldEqual stringToXml(internalServerError)
        header(XConversationIdHeaderName, result).get shouldNot be("")
      }
    }
  }


}
