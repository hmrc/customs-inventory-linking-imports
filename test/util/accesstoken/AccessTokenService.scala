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

package util.accesstoken

import org.openqa.selenium.{By, WebDriver}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.Inject

class AccessTokenService @Inject() (servicesConfig: ServicesConfig) {

  implicit val webDriver: WebDriver = Env.driver
  lazy val params: AccessTokenRequest = accessTokenRequest()

  def accessToken(scope: String): Option[String] = {

    accessAuthorisationPage(params, scope)

    enterUsernamePassword(params)

    authoriseEndpoint()

    extractCode(webDriver.getCurrentUrl, "code")
  }

  def accessAuthorisationPage(params: AccessTokenRequest, scope: String): Unit = {
    webDriver.get(authoriseUrl(params.host, params.clientId, scope))
  }

  def enterUsernamePassword(params: AccessTokenRequest): Unit = {
    webDriver.findElement(By.id("userId")).sendKeys(params.username)
    val passwordField = webDriver.findElement(By.id("password"))
    passwordField.sendKeys(params.password)
    passwordField.submit()
  }

  def authoriseEndpoint(): Unit = {
    webDriver.findElement(By.id("authorise")).click()
  }

  private def authoriseUrl(host: String, clientId: String, scope: String): String = {

    s"""$host/oauth/authorize
       |?client_id=$clientId
       |&scope=$scope
       |&response_type=code
       |&redirect_uri=http://localhost:9000""".stripMargin.replaceAll("\n", "")
  }

  def extractCode(url: String, parameterName: String): Option[String] = {
    val parameters: Array[String] = new URL(url).getQuery.split("&")
    parameters find (_ startsWith s"$parameterName=") map {
      s => s.split("=")(1)
    }
  }

  private def accessTokenRequest(): AccessTokenRequest = {
    AccessTokenRequest(
      host = servicesConfig.getConfString("accessToken.oauthFrontendHost", throw new IllegalStateException("host is missing"))
      , clientId = servicesConfig.getConfString("accessToken.clientId", throw new IllegalStateException("clientId is missing"))
      , username = servicesConfig.getConfString("accessToken.username", throw new IllegalStateException("username is missing"))
      , password = servicesConfig.getConfString("accessToken.password", throw new IllegalStateException("password is missing"))
    )
  }

  case class AccessTokenRequest(host: String, clientId: String, username: String, password: String)

}
