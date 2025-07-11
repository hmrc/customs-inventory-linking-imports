/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.customs.inventorylinking.imports.controllers.dynamicDefinitionControllers

import controllers.Assets
import play.api.Configuration
import play.api.http.MimeTypes
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.definitionControllers.DocumentationController

import javax.inject.{Inject, Singleton}

@Singleton
class ApiDocumentationController @Inject()(assets: Assets,
                                           cc: ControllerComponents,
                                           configuration: Configuration)
  extends DocumentationController(assets, cc) {

  private lazy val v1Enabled = configuration.getOptional[Boolean]("api.access.version-1.0.enabled").getOrElse(true)
  private lazy val v2Enabled = configuration.getOptional[Boolean]("api.access.version-2.0.enabled").getOrElse(true)

  def definition(): Action[AnyContent] = Action {
    Ok(uk.gov.hmrc.customs.inventorylinking.imports.views.txt.definition(v1Enabled, v2Enabled)).as(MimeTypes.JSON)
  }
}
