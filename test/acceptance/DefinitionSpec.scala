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

import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class DefinitionSpec extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite with Matchers {

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(Map(
    "api.access.version-1.0.whitelistedApplicationIds.0" -> "someId-1",
    "api.access.version-1.0.whitelistedApplicationIds.1" -> "someId-2"
  )).build()

  feature("Ensure definition file") {

    scenario("is correct") {

      Given("the API is available")
      val request = FakeRequest("GET", "/api/definition")

      When("api definition is requested")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 200 status is received")
      val resultFuture = result.get
      status(resultFuture) shouldBe OK

      And("the response body is correct")
      contentAsJson(resultFuture) shouldBe Json.parse(
        """
         |{
         |  "scopes": [
         |    {
         |      "key": "write:customs-il-imports-movement-validation",
         |      "name": "Customs Inventory Linking Imports - Movement Validation",
         |      "description": "Send validated movement response messages provided by the CSP as a result of a previous validate movement request"
         |    },
         |    {
         |      "key": "write:customs-il-imports-arrival-notifications",
         |      "name": "Customs Inventory Linking Imports - Arrival Notifications",
         |      "description": "CSPs requesting to present their goods to Customs"
         |    }
         |  ],
         |  "api": {
         |    "name": "Customs Inventory Linking Imports",
         |    "description": "Single WCO-compliant Customs Inventory Linking Import Declaration API",
         |    "context": "customs/inventory-linking-imports",
         |    "versions": [
         |      {
         |        "version": "1.0",
         |        "status": "BETA",
         |        "endpointsEnabled": true,
         |        "access": {
         |          "type": "PRIVATE",
         |          "whitelistedApplicationIds": [
         |            "someId-1",
         |            "someId-2"
         |          ]
         |        },
         |        "fieldDefinitions": [
         |          {
         |            "name": "callbackUrl",
         |            "description": "The URL of your HTTPS webservice that HMRC calls to notify you regarding request submission.",
         |            "type": "URL",
         |            "hint": "This is how we'll notify you when we've processed them. It must include https and port 443"
         |          },
         |          {
         |            "name": "securityToken",
         |            "description": "The full value of Authorization HTTP header that will be used when notifying you.",
         |            "type": "SecureToken",
         |            "hint": "For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk"
         |          }
         |        ]
         |      }
         |    ]
         |  }
         |}
        """.stripMargin)
    }
  }
}
