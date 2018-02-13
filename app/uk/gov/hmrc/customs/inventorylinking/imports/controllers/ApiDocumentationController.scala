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

package uk.gov.hmrc.customs.inventorylinking.imports.controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.http.HttpErrorHandler
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.customs.api.common.controllers.DocumentationController
import uk.gov.hmrc.customs.inventorylinking.imports.views.txt

class ApiDocumentationController @Inject()(httpErrorHandler: HttpErrorHandler, configuration: Configuration) extends DocumentationController(httpErrorHandler) {

  private val apiScopeConfigKey = "customs.definition.api-scope"
  private lazy val apiScopeKey = configuration.getString(apiScopeConfigKey).getOrElse(throw new IllegalStateException(s"$apiScopeConfigKey is not configured"))

  def definition(): Action[AnyContent] = Action {
    Ok(txt.definition(apiScopeKey)).withHeaders(CONTENT_TYPE -> JSON)
  }
}
