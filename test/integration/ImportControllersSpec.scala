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

package integration

import com.github.tomakehurst.wiremock.client.WireMock.{status => _, _}
import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import unit.util.XMLTestData.ValidInventoryLinkingMovementRequestXML
import util.TestData.{XBadgeIdentifierHeaderName, XClientIdHeaderName, XCorrelationIdHeaderName}
import util.{ExternalServicesConfig, TestData, WireMockRunner}

import scala.util.control.NonFatal
import scala.xml.{Node, Utility, XML}

class ImportControllersSpec extends IntegrationTestSpec with GivenWhenThen with GuiceOneAppPerSuite
  with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with WireMockRunner with OptionValues with TableDrivenPropertyChecks {

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  val payload = <import>payload</import>
  private val movementValidationUrl = "/movement-validation"
  private val arrivalNotificationUrl = "/arrival-notifications"

  val testEndpoints = Table(
    "endpoint",
    movementValidationUrl,
    arrivalNotificationUrl
  )

  val missingHeaders = Table(
    ( "header", "responseCode", "invalid"),
    ( ACCEPT, NOT_ACCEPTABLE, MimeTypes.JSON),
    ( XClientIdHeaderName, INTERNAL_SERVER_ERROR, "ABC"),
    ( XBadgeIdentifierHeaderName , BAD_REQUEST, "ABC_123_DEF_456")
  )

  val invalidHeaders = Table(
    ( "header", "responseCode", "invalid"),
    ( ACCEPT, NOT_ACCEPTABLE, MimeTypes.JSON),
    ( CONTENT_TYPE, UNSUPPORTED_MEDIA_TYPE, MimeTypes.JSON),
    ( XClientIdHeaderName, INTERNAL_SERVER_ERROR, "ABC"),
    ( XBadgeIdentifierHeaderName, BAD_REQUEST, "ABC_123_DEF_456")
  )

  private val validMessageMatcher = post(urlMatching(movementValidationUrl)).
    withRequestBody(equalToXml(ValidInventoryLinkingMovementRequestXML.toString())).
    withHeader(ACCEPT, equalTo(MimeTypes.XML)).
    withHeader(CONTENT_TYPE, equalTo(MimeTypes.XML)).
    withHeader(DATE, notMatching("")).
    withHeader(XCorrelationIdHeaderName, notMatching("")).
    withHeader(X_FORWARDED_HOST, equalTo("MDTP")).
    withHeader(AUTHORIZATION, equalTo(s"Bearer ${ExternalServicesConfig.AuthToken}"))



    "ImportControllers when validating headers" should {
      forAll(testEndpoints) { (endpoint: String) =>
      forAll(missingHeaders) { (header: String, responseCode: Int, invalid: String) =>

        s"Send a HTTP $responseCode when $header header is missing for $endpoint" in {

          stubFor(validMessageMatcher willReturn aResponse().withStatus(ACCEPTED))
          val result = postValidMovementMessageWithoutHeader(header, endpoint)
          status(result) shouldBe responseCode
        }
      }

      forAll(invalidHeaders) { (header: String, responseCode: Int, invalid: String) =>

        s"Send a HTTP $responseCode when $header is invalid for $endpoint" in {

          stubFor(validMessageMatcher willReturn aResponse().withStatus(ACCEPTED))
          val result = postValidMovementMessageWithHeader(header, invalid, endpoint)
          status(result) shouldBe responseCode
        }
      }
    }
  }
  protected def string2xml(s: String): Node = {
    val xml = try {
      XML.loadString(s)
    } catch {
      case NonFatal(thr) => fail("Not an xml: " + s, thr)
    }
    Utility.trim(xml)
  }

  private def postValidMovementMessageWithoutHeader(missingHeader: String, endpoint: String) = {
    val request = FakeRequest("POST", endpoint)
      .withXmlBody(ValidInventoryLinkingMovementRequestXML)
      .withHeaders((TestData.Headers.validHeaders - missingHeader).toSeq: _*)

    route(app, request).get
  }

  private def postValidMovementMessageWithHeader(headerName: String, headerValue: String, endpoint: String) = {
    val request = FakeRequest("POST", endpoint)
      .withXmlBody(ValidInventoryLinkingMovementRequestXML)
      .withHeaders((TestData.Headers.validHeaders + (headerName -> headerValue)).toSeq: _*)
    route(app, request).get
  }
}
