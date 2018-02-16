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

import java.io.FileNotFoundException

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

  private lazy val applicationRamlContent = getResourceFileContent("/public/api/conf/1.0/application.raml")
  private val apiScope = "scope"
  private lazy val config = Configuration("customs.definition.api-scope" -> apiScope)
  private lazy val controller = new ApiDocumentationController(DefaultHttpErrorHandler, config)

  "with empty configuration DocumentationController.defintion" should {
    "throw IllegalStateException" in {
      val controller = new ApiDocumentationController(DefaultHttpErrorHandler, Configuration())
      val thrown = the[IllegalStateException] thrownBy getDefinition(controller)
      thrown.getMessage should equal("customs.definition.api-scope is not configured")
    }
  }

  "With valid configuration ApiDocumentationController.definition" should {
    val result = getDefinition(controller)

    "return OK status" in {
      status(result) shouldBe OK
    }

    "have a JSON content type" in {
      result.header.headers should contain (CONTENT_TYPE -> "application/json;charset=utf-8")
    }

    "return definition in the body" in {
      jsonBodyOf(result) shouldBe Json.parse(txt.definition("scope", Seq.empty).toString())
    }
  }

  "With valid configuration ApiDocumentationController.conf" should {
    lazy val result = getDocumentation(controller)

    "return OK status" in {
      status(result) shouldBe OK
    }

    "return application.raml in the body" in {
      bodyOf(result) shouldBe applicationRamlContent
    }
  }

  private def getDefinition(controller: ApiDocumentationController) = {
    await(controller.definition().apply(FakeRequest()))
  }

  private def getDocumentation(controller: ApiDocumentationController) = {
    await(controller.conf("1.0","application.raml").apply(FakeRequest("GET", "/api/conf/1.0/application.raml")))
  }

  private def getResourceFileContent(resourceFile: String): String = {
    val is = Option(getClass.getResourceAsStream(resourceFile)).getOrElse(
      throw new FileNotFoundException(s"Resource file not found: $resourceFile"))
    scala.io.Source.fromInputStream(is).mkString
  }


}
