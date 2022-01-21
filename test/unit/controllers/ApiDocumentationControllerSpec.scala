/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ApiDocumentationController
import uk.gov.hmrc.customs.inventorylinking.imports.views.txt
import util.UnitSpec

class ApiDocumentationControllerSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  private implicit val materializer: Materializer = app.materializer
  private lazy val applicationRamlContent = getResourceFileContent("/public/api/conf/1.0/application.raml")
  private lazy val controller = app.injector.instanceOf[ApiDocumentationController]

  override def fakeApplication(): Application  = new GuiceApplicationBuilder().configure(Map(
    "api.access.version-1.0.enabled" -> "false",
    "api.access.version-2.0.enabled" -> "false"
  )).build()

  "With valid configuration ApiDocumentationController.definition" should {
    val result = getDefinition(controller)

    "return OK status" in {
      status(result) shouldBe OK
    }

    "return definition in the body with v1 and v2 disabled" in {
      jsonBodyOf(result) shouldBe Json.parse(txt.definition(false, false).toString())
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
