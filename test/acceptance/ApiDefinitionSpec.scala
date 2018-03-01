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
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ApiDefinitionSpec extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite with Matchers {
  override implicit lazy val app: Application = new GuiceApplicationBuilder().build()

  feature("API Definition") {
    scenario("can be read") {
      Given("the API is available")

      When("the api definition is requested")
      val request = FakeRequest("GET", "/api/definition")
      val response = route(app = app, request).get

      Then("a response status of 200 OK is received")
      status(response) shouldBe OK

      And("the response body defines the API")
      contentAsJson(response) shouldBe Json.parse(
        """
          |{
          |  "scopes": [
          |    {
          |      "key": "write:customs-inventory-linking-imports",
          |      "name": "Customs Inventory Linking Imports",
          |      "description": "Submit a Customs Inventory Linking Import Declaration"
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
          |          "type": "PUBLIC",
          |          "whitelistedApplicationIds": []
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
