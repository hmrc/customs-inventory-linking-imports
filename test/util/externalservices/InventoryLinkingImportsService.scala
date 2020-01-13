/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.http.MimeTypes
import play.api.test.Helpers.{ACCEPT, ACCEPTED, AUTHORIZATION, CONTENT_TYPE, DATE, XML, X_FORWARDED_HOST}
import util.TestData._
import util.{ExternalServicesConfig, WireMockRunner}

trait InventoryLinkingImportsService extends WireMockRunner {

  def startImportsService(requestPath: String): Unit = {
    setupBackendServiceToReturn(requestPath, ACCEPTED)

    stubFor(post(requestPath).
      willReturn(
        aResponse()
          .withStatus(ACCEPTED)))
  }

  def verifyImportsConnectorServiceWasCalledWith(requestPath: String,
                                                 requestBody: String,
                                                 expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                                 maybeUnexpectedAuthToken: Option[String] = None) {
    verify(1, postRequestedFor(urlMatching(requestPath))
      .withHeader(CONTENT_TYPE, equalTo(ConnectorContentTypeHeaderValue))
      .withHeader(ACCEPT, equalTo(XML))
      .withHeader(AUTHORIZATION, equalTo(s"Bearer $expectedAuthToken"))
      .withHeader(DATE, notMatching(""))
      .withHeader(XCorrelationIdHeaderName, notMatching(""))
      .withHeader(XConversationIdHeaderName, notMatching(""))
      .withHeader(X_FORWARDED_HOST, equalTo("MDTP"))
      .withRequestBody(equalToXml(requestBody))
    )

    maybeUnexpectedAuthToken foreach { unexpectedAuthToken =>
      verify(0, postRequestedFor(urlMatching(requestPath)).withHeader(AUTHORIZATION, equalTo(s"Bearer $unexpectedAuthToken")))
    }
  }

  def setupBackendServiceToReturn(requestPath: String, status: Int): Unit =
    stubFor(post(urlMatching(requestPath))
        .withHeader(ACCEPT, equalTo(MimeTypes.XML))
        .withHeader(CONTENT_TYPE, equalTo(s"${MimeTypes.XML}; charset=UTF-8"))
        .withHeader(DATE, notMatching(""))
        .withHeader(XCorrelationIdHeaderName, notMatching(""))
        .withHeader(XConversationIdHeaderName, notMatching(""))
        .withHeader(X_FORWARDED_HOST, equalTo("MDTP"))
        .withHeader(AUTHORIZATION, equalTo(s"Bearer ${ExternalServicesConfig.AuthToken}"))
          willReturn aResponse()
            .withStatus(status))

}
