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

package util.externalservices

import java.net.URLEncoder

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.Helpers.OK
import util.ApiSubscriptionFieldsTestData._
import util.WireMockRunner
import util.externalservices.InventoryLinkingImportsExternalServicesConfig.ApiSubscriptionFieldsContext

trait ApiSubscriptionFieldsService extends WireMockRunner {

  private val apiContextEncoded = URLEncoder.encode("customs/inventory-linking-imports", "UTF-8")
  private val version = "1.0"

  def getSubscriptionFields(clientId: String): String = {
    s"$ApiSubscriptionFieldsContext/application/$clientId/context/$apiContextEncoded/version/$version"
  }

  private def apiSubsUrl(clientId: String): String = getSubscriptionFields(clientId)

  private def urlMatchingRequestPath(clientId: String) = {
    urlEqualTo(apiSubsUrl(clientId))
  }

  def startApiSubscriptionFieldsService(clientId: String): Unit = setupGetSubscriptionFieldsToReturn(OK, clientId)

  def setupGetSubscriptionFieldsToReturn(status: Int = OK, clientId: String = TestXClientId): Unit = {
    stubFor(get(urlMatchingRequestPath(clientId)).
      willReturn(
        aResponse()
          .withBody(ResponseJsonString)
          .withStatus(status))
    )
  }

  def verifyGetSubscriptionFieldsCalled(clientId: String): Unit = {
    verify(1, getRequestedFor(urlMatchingRequestPath(clientId)))
  }

  def verifyGetSubscriptionFieldsNotCalled(clientId: String): Unit = {
    verify(0, getRequestedFor(urlMatchingRequestPath(clientId)))
  }
}
