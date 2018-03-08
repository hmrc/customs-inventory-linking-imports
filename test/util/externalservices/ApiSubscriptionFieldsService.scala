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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.Helpers.OK
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.ApiSubscriptionFieldsPath._
import uk.gov.hmrc.customs.inventorylinking.imports.model.ApiSubscriptionKey
import util.ApiSubscriptionFieldsTestData._
import util.WireMockRunner
import util.externalservices.InventoryLinkingImportsExternalServicesConfig.ApiSubscriptionFieldsContext

trait ApiSubscriptionFieldsService extends WireMockRunner {
  private def apiSubsUrl(apiSubsKey: ApiSubscriptionKey) = url(ApiSubscriptionFieldsContext, apiSubsKey)
  private def urlMatchingRequestPath(apiSubs: ApiSubscriptionKey) = {
    urlEqualTo(apiSubsUrl(apiSubs))
  }

  def startApiSubscriptionFieldsService(apiSubsKey: ApiSubscriptionKey): Unit =
    setupGetSubscriptionFieldsToReturn(OK, apiSubsKey)

  def setupGetSubscriptionFieldsToReturn(status: Int = OK, apiSubsKey: ApiSubscriptionKey = TestApiSubscriptionKey): Unit = {
    stubFor(get(urlMatchingRequestPath(apiSubsKey: ApiSubscriptionKey)).
      willReturn(
        aResponse()
          .withBody(ResponseJsonString)
          .withStatus(status))
    )
  }

  def verifyGetSubscriptionFieldsCalled(apiSubsKey: ApiSubscriptionKey = TestApiSubscriptionKey): Unit = {
    verify(1, getRequestedFor(urlMatchingRequestPath(apiSubsKey: ApiSubscriptionKey)))
  }

  def verifyGetSubscriptionFieldsNotCalled(apiSubsKey: ApiSubscriptionKey = TestApiSubscriptionKey): Unit = {
    verify(0, getRequestedFor(urlMatchingRequestPath(apiSubsKey: ApiSubscriptionKey)))
  }
}
