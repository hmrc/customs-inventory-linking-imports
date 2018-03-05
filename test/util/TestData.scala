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

package util

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import play.api.test.Helpers.{ACCEPT, CONTENT_TYPE}
import uk.gov.hmrc.customs.api.common.config.ServiceConfig
import uk.gov.hmrc.customs.inventorylinking.imports.model.RequestInfo
import util.XMLTestData.{ValidInventoryLinkingGoodsArrivalRequestXML, ValidInventoryLinkingMovementRequestXML, _}
import play.api.http.MimeTypes.{XML, JSON}

import scala.util.Random
import scala.xml.Elem

object TestData {

  val XBadgeIdentifierHeaderName = "X-Badge-Identifier"
  val XClientIdHeaderName = "X-Client-ID"
  val XConversationIdHeaderName = "X-Conversation-ID"
  val XCorrelationIdHeaderName = "X-Correlation-ID"

  val conversationId: UUID = UUID.fromString("a26a559c-9a1c-42c5-a164-6508beea7749")
  val correlationId: UUID = UUID.fromString("954e2369-3bfa-4aaa-a2a2-c4700e3f71ec")
  val XClientIdHeaderValue = "c9503c3d-6df7-448d-a01b-e623a3b8806d"
  val XBadgeIdentifierHeaderValue = "ABC123"
  val AcceptHeaderValue = "application/vnd.hmrc.1.0+xml"
  val ConnectorContentTypeHeaderValue = s"$XML; charset=UTF-8"

  lazy val InvalidAcceptHeader = ACCEPT -> JSON
  lazy val InvalidContentTypeHeader = CONTENT_TYPE -> JSON
  lazy val InvalidXClientIdHeader = XClientIdHeaderName -> "This is not a UUID"
  lazy val InvalidXBadgeIdentifier = XBadgeIdentifierHeaderName -> "This is too long and has spaces _"

  lazy val ValidAcceptHeader = ACCEPT -> AcceptHeaderValue
  lazy val ValidContentTypeHeader = CONTENT_TYPE -> XML
  lazy val ValidXClientIdHeader = XClientIdHeaderName -> XClientIdHeaderValue
  lazy val ValidXBadgeIdentifierHeader = XBadgeIdentifierHeaderName -> XBadgeIdentifierHeaderValue
  lazy val XConversationIdHeader: (String, String) = XConversationIdHeaderName -> conversationId.toString
  val ValidHeaders = Map(ValidAcceptHeader, ValidContentTypeHeader, ValidXClientIdHeader, ValidXBadgeIdentifierHeader)

  val validBasicAuthToken = s"Basic ${Random.alphanumeric.take(18).mkString}=="

  val cspBearerToken = "CSP-Bearer-Token"
  val nonCspBearerToken = "Software-House-Bearer-Token"

  val validateMovementsXsdElementName = "InventoryLinkingImportsValidateMovementResponse"

  val validateMovementsXsdLocations = List(
    "/api/conf/1.0/schemas/imports/inventoryLinkingImportValidateMovementResponse.xsd",
    "/api/conf/1.0/schemas/imports/inventoryLinkingImportCommonTypes.xsd"
  )

  val goodsArrivalXsdElementName= "inventoryLinkingImportsGoodsArrival"

  val goodsArrivalXsdLocations = List(
    "/api/conf/1.0/schemas/imports/inventoryLinkingImportArriveGoods.xsd",
    "/api/conf/1.0/schemas/imports/inventoryLinkingImportCommonTypes.xsd"
  )

  val requestDateTime: DateTime = new DateTime(2017, 6, 8, 13, 55, 0, 0, DateTimeZone.UTC)
  val requestDateTimeHttp: String = "2017-06-08T13:55:00Z"
  val requestInfo: RequestInfo = RequestInfo(conversationId, correlationId, requestDateTime)
  val bearerToken: String = "token"
  val serviceConfig: ServiceConfig = ServiceConfig("url", Some(bearerToken), "env")

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")

  val body: Elem = <payload>payload</payload>
  val decoratedBody = <wrapped><payload>payload</payload></wrapped>

  val ValidValidateMovementRequest = FakeRequest("POST", "/movement-validation")
    .withXmlBody(ValidInventoryLinkingMovementRequestXML)
    .withHeaders(ValidHeaders.toSeq: _*)

  val ValidGoodsArrivalRequest = FakeRequest("POST", "/arrival-notifications")
    .withXmlBody(ValidInventoryLinkingGoodsArrivalRequestXML)
    .withHeaders(ValidHeaders.toSeq: _*)

  lazy val InvalidRequest: FakeRequest[AnyContentAsXml] = ValidValidateMovementRequest.withXmlBody(InvalidInventoryLinkingGoodsArrivalRequestXML)
  lazy val InvalidRequestWithMalformedXml: FakeRequest[String] = ValidValidateMovementRequest.withBody("<xm> malformed xml <xm>")
  lazy val InvalidRequestWithNonXml: FakeRequest[String] = ValidValidateMovementRequest.withBody("""  {"valid": "json payload" }  """)

}
