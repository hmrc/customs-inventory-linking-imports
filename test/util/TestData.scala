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

import com.google.inject.AbstractModule
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.MimeTypes.{JSON, XML}
import play.api.inject.guice.GuiceableModule
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import play.api.test.Helpers.{ACCEPT, CONTENT_TYPE, POST}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{AuthProviders, Enrolment}
import uk.gov.hmrc.customs.api.common.config.ServiceConfig
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ConversationIdRequest, ExtractedHeadersImpl, ValidatedHeadersRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{UniqueIdsService, UuidService}
import util.XMLTestData.{ValidInventoryLinkingGoodsArrivalRequestXML, ValidInventoryLinkingMovementRequestXML}

import scala.util.Random
import scala.xml.{Elem, NodeSeq}

object TestData {

  val XBadgeIdentifierHeaderName = "X-Badge-Identifier"
  val XClientIdHeaderName = "X-Client-ID"
  val XConversationIdHeaderName = "X-Conversation-ID"
  val XCorrelationIdHeaderName = "X-Correlation-ID"

  val conversationIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  val conversationIdUuid: UUID = UUID.fromString(conversationIdValue)
  val conversationId: ConversationId = ConversationId(conversationIdUuid)

  val correlationIdValue = "e61f8eee-812c-4b8f-b193-06aedc60dca2"
  val correlationIdUuid: UUID = UUID.fromString(correlationIdValue)
  val correlationId = CorrelationId(correlationIdUuid)

  val validBadgeIdentifierValue = "BADGEID123"
  val invalidBadgeIdentifierValue = "INVALIDBADGEID123456789"
  val invalidBadgeIdentifier: BadgeIdentifier = BadgeIdentifier(invalidBadgeIdentifierValue)
  val badgeIdentifier: BadgeIdentifier = BadgeIdentifier(validBadgeIdentifierValue)

  val AcceptHeaderValue = "application/vnd.hmrc.1.0+xml"
  val ConnectorContentTypeHeaderValue = s"$XML; charset=UTF-8"

  val TestExtractedHeaders = ExtractedHeadersImpl(BadgeIdentifier(validBadgeIdentifierValue), ApiSubscriptionFieldsTestData.clientId)

  lazy val InvalidAcceptHeader = ACCEPT -> JSON
  lazy val InvalidContentTypeJsonHeader = CONTENT_TYPE -> JSON
  lazy val InvalidXClientIdHeader = XClientIdHeaderName -> "This is not a UUID"
  lazy val InvalidXBadgeIdentifier = XBadgeIdentifierHeaderName -> "This is too long and has spaces _"

  lazy val ValidAcceptHeader: (String, String) = ACCEPT -> AcceptHeaderValue
  lazy val ValidContentTypeHeader: (String, String) = CONTENT_TYPE -> (XML + "; charset=utf-8")
  lazy val ValidXClientIdHeader: (String, String) = XClientIdHeaderName -> ApiSubscriptionFieldsTestData.clientId.value
  lazy val ValidXBadgeIdentifierHeader: (String, String) = XBadgeIdentifierHeaderName -> validBadgeIdentifierValue
  lazy val XConversationIdHeader: (String, String) = XConversationIdHeaderName -> ConversationId.toString
  val ValidHeaders = Map(ValidAcceptHeader, ValidContentTypeHeader, ValidXClientIdHeader, ValidXBadgeIdentifierHeader)

  val validBasicAuthToken = s"Basic ${Random.alphanumeric.take(18).mkString}=="

  val cspBearerToken = "CSP-Bearer-Token"

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

  def elementName(messageType: ImportsMessageType): String = messageType match {
    case _: GoodsArrival => goodsArrivalXsdElementName
    case _: ValidateMovement => validateMovementsXsdElementName
  }

  def otherElementName(messageType: ImportsMessageType): String = messageType match {
    case _: GoodsArrival => validateMovementsXsdElementName
    case _: ValidateMovement => goodsArrivalXsdElementName
  }

  val requestDateTime: DateTime = new DateTime(2017, 6, 8, 13, 55, 0, 0, DateTimeZone.UTC)
  val requestDateTimeHttp: String = "2017-06-08T13:55:00Z"
  val bearerToken: String = "token"
  val serviceConfig: ServiceConfig = ServiceConfig("url", Some(bearerToken), "env")

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")

  val outgoingBody: NodeSeq = <payload>payload</payload>
  val decoratedBody = <wrapped><payload>payload</payload></wrapped>

  val ValidValidateMovementRequest = FakeRequest("POST", "/movement-validation")
    .withXmlBody(ValidInventoryLinkingMovementRequestXML)
    .withHeaders(ValidHeaders.toSeq: _*)

  val ValidGoodsArrivalRequest = FakeRequest("POST", "/arrival-notifications")
    .withXmlBody(ValidInventoryLinkingGoodsArrivalRequestXML)
    .withHeaders(ValidHeaders.toSeq: _*)

  val GoodsArrivalAuthPredicate: Predicate = Enrolment("write:customs-il-imports-arrival-notifications") and AuthProviders(PrivilegedApplication)
  val ValidateMovementAuthPredicate: Predicate = Enrolment("write:customs-il-imports-movement-validation") and AuthProviders(PrivilegedApplication)

  object TestModule extends AbstractModule {
    def configure(): Unit = {
      bind(classOf[UuidService]) toInstance mockUuidService
    }

    def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
  }

  val mockUuidService: UuidService = MockitoSugar.mock[UuidService]
  val stubUniqueIdsService = new UniqueIdsService(mockUuidService) {
    override def conversation: ConversationId = conversationId
    override def correlation: CorrelationId = correlationId
  }

  val TestXmlPayload: Elem = <foo>bar</foo>
  val TestFakeRequest: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload)
  val TestConversationIdRequest = ConversationIdRequest(conversationId, TestFakeRequest)
  val TestValidatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestAuthorisedRequest = TestValidatedHeadersRequest.toAuthorisedRequest
  val TestCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toAuthorisedRequest.toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestValidatedHeadersRequestNoBadge: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)

  def testFakeRequest(badgeIdString: String = badgeIdentifier.value): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload)

  implicit class FakeRequestOps[R](val fakeRequest: FakeRequest[R]) extends AnyVal {
    def fromCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $cspBearerToken")

    def postTo(endpoint: String): FakeRequest[R] = fakeRequest.copyFakeRequest(method = POST, uri = endpoint)
  }
}
