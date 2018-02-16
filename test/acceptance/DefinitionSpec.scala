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
    "api.access.version-2.0.whitelistedApplicationIds.0" -> "someId-1",
    "api.access.version-2.0.whitelistedApplicationIds.1" -> "someId-2"
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
         |      "key": "write:customs-inventory-linking-imports",
         |      "name": "Customs Inventory Linking Imports",
         |      "description": "Submit a Customs Inventory Linking Import Declaration"
         |    }
         |  ],
         |  "api": {
         |    "name": "Customs Inventory Linking Imports",
         |    "description": "Single WCO-compliant Customs Inventory Linking Import Declaration API",
         |    "context": "/customs/consignments",
         |    "versions": [
         |      {
         |        "version": "1.0",
         |        "status": "BETA",
         |        "endpointsEnabled": true,
         |        "access": {
         |          "type": "PUBLIC",
         |          "whitelistedApplicationIds": [
         |            "someId-1",
         |            "someId-2"
         |          ]
         |        }
         |      }
         |    ]
         |  }
         |}
        """.stripMargin)
    }
  }
}