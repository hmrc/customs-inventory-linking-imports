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

package unit.controllers

import akka.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ApiDocumentationController
import uk.gov.hmrc.customs.inventorylinking.imports.views.txt
import uk.gov.hmrc.play.test.UnitSpec

class ApiDocumentationControllerSpec extends UnitSpec with GuiceOneAppPerSuite {
  implicit val materializer: Materializer = app.materializer

  "with empty configuration DocumentationController.defintion" should {
    "throw IllegalStateException" in {
      val controller = new ApiDocumentationController(DefaultHttpErrorHandler, Configuration())
      val thrown = the[IllegalStateException] thrownBy getDefinition(controller)
      thrown.getMessage should equal("customs.definition.api-scope is not configured")
    }
  }

  "With valid configuration DocumentationController.definition" should {
    val apiScope = "scope"
    val config = Configuration("customs.definition.api-scope" -> apiScope)
    val controller = new ApiDocumentationController(DefaultHttpErrorHandler, config)
    val result = getDefinition(controller)

    "return OK status" in {
      status(result) shouldBe OK
    }

    "have a JSON content type" in {
      result.header.headers should contain (CONTENT_TYPE -> "application/json;charset=utf-8")
    }

    "return definition in the body" in {
      jsonBodyOf(result) shouldBe Json.parse(txt.definition("scope").toString())
    }
  }

  private def getDefinition(controller: ApiDocumentationController) = {
    await(controller.definition().apply(FakeRequest()))
  }
}
