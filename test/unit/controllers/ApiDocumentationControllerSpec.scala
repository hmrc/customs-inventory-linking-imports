/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.controllers

import controllers.Assets
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ApiDocumentationController

class ApiDocumentationControllerSpec extends PlaySpec with MockitoSugar with Results with BeforeAndAfterEach {

  private val mockService = mock[HttpErrorHandler]

  private val v1AndV2Disabled = Map(
    "metrics.enabled" -> false,
    "api.access.version-1.0.enabled" -> "false",
    "api.access.version-2.0.enabled" -> "false")

  private def getApiDefinitionWith(configMap: Map[String, Any]) =
    new ApiDocumentationController(mock[Assets], Helpers.stubControllerComponents(), play.api.Configuration.from(configMap))
      .definition()

  override def beforeEach() {
    reset(mockService)
  }

  "API Definition" should {

    "be correct when V1 is not enabled and V2 is not enabled" in {
      val result = getApiDefinitionWith(v1AndV2Disabled)(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(false, false)
    }

  }

  private def expectedJson(v1Enabled: Boolean, v2Enabled: Boolean) = {
    Json.parse(
      s"""
         |{
         |  "scopes":[
         |    {
         |      "key":"write:customs-il-imports-movement-validation",
         |      "name":"Customs Inventory Linking Imports - Movement Validation",
         |      "description":"Send validated movement response messages provided by the CSP as a result of a previous validate movement request"
         |    },
         |    {
         |      "key":"write:customs-il-imports-arrival-notifications",
         |      "name":"Customs Inventory Linking Imports - Arrival Notifications",
         |      "description":"CSPs requesting to present their goods to Customs"
         |    }
         |  ],
         |  "api":{
         |    "name":"Customs Inventory Linking Imports",
         |    "description":"Single WCO-compliant Customs Inventory Linking Import Declaration API",
         |    "context":"customs/inventory-linking-imports",
         |    "versions":[
         |      {
         |        "version":"1.0",
         |        "status":"BETA",
         |        "endpointsEnabled":$v1Enabled,
         |        "access":{
         |          "type":"PRIVATE"
         |        },
         |        "fieldDefinitions":[
         |          {
         |            "name":"callbackUrl",
         |            "description":"The URL of your HTTPS webservice that HMRC calls to notify you regarding request submission.",
         |            "type":"URL",
         |            "hint":"This is how we'll notify you when we've processed them. It must include https and port 443",
         |            "shortDescription":"Callback URL",
         |            "validation":{
         |              "errorMessage":"Enter a URL in the correct format, like 'https://your.domain.name/some/path' ",
         |              "rules":[
         |                {
         |                  "UrlValidationRule":{}
         |                }
         |              ]
         |            }
         |          },
         |          {
         |            "name":"securityToken",
         |            "description":"The full value of Authorization HTTP header that will be used when notifying you.",
         |            "type":"SecureToken",
         |            "hint":"For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk",
         |            "shortDescription":"Authorization Token"
         |          },
         |          {
         |            "name":"authenticatedEori",
         |            "description":"What's your Economic Operator Registration and Identification (EORI) number?",
         |            "type":"STRING",
         |            "hint":"This is your EORI that will associate your application with you as a CSP",
         |            "shortDescription":"EORI"
         |          }
         |        ]
         |      },
         |      {
         |        "version":"2.0",
         |        "status":"BETA",
         |        "endpointsEnabled":$v2Enabled,
         |        "access":{
         |          "type":"PRIVATE"
         |        },
         |        "fieldDefinitions":[
         |          {
         |            "name":"callbackUrl",
         |            "description":"The URL of your HTTPS webservice that HMRC calls to notify you regarding request submission.",
         |            "type":"URL",
         |            "hint":"This is how we'll notify you when we've processed them. It must include https and port 443",
         |            "shortDescription":"Callback URL",
         |            "validation":{
         |              "errorMessage":"Enter a URL in the correct format, like 'https://your.domain.name/some/path' ",
         |              "rules":[
         |                {
         |                  "UrlValidationRule":{}
         |                }
         |              ]
         |            }
         |          },
         |          {
         |            "name":"securityToken",
         |            "description":"The full value of Authorization HTTP header that will be used when notifying you.",
         |            "type":"SecureToken",
         |            "hint":"For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk",
         |            "shortDescription":"Authorization Token"
         |          },
         |          {
         |            "name":"authenticatedEori",
         |            "description":"What's your Economic Operator Registration and Identification (EORI) number?",
         |            "type":"STRING",
         |            "hint":"This is your EORI that will associate your application with you as a CSP",
         |            "shortDescription":"EORI"
         |          }
         |        ]
         |      }
         |    ]
         |  }
         |}
      """.stripMargin)
  }

}

