/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
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
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders._
import uk.gov.hmrc.customs.inventorylinking.imports.services.{UniqueIdsService, UuidService}
import util.XMLTestData.{ValidInventoryLinkingGoodsArrivalRequestXML, ValidInventoryLinkingMovementRequestXML}

import scala.xml.Elem

object TestData {

  val XBadgeIdentifierHeaderName = "X-Badge-Identifier"
  val XClientIdHeaderName = "X-Client-ID"
  val XConversationIdHeaderName = "X-Conversation-ID"
  val XCorrelationIdHeaderName = "X-Correlation-ID"

  val ConversationIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  val ConversationIdUuid: UUID = UUID.fromString(ConversationIdValue)
  val ValidConversationId: ConversationId = ConversationId(ConversationIdUuid)

  val CorrelationIdValue = "e61f8eee-812c-4b8f-b193-06aedc60dca2"
  val CorrelationIdUuid: UUID = UUID.fromString(CorrelationIdValue)
  val ValidCorrelationId = CorrelationId(CorrelationIdUuid)

  val CorrelationIdHeaderValue = "abCabC123u"
  val ValidCorrelationIdHeader = CorrelationIdHeader(CorrelationIdHeaderValue)

  val AuthenticatedEoriValue = "RASHADMUGHAL"
  val ValidAuthenticatedEori = AuthenticatedEori(AuthenticatedEoriValue)

  val ValidBadgeIdentifierValue = "BADGEID123"
  val InvalidBadgeIdentifierValue = "INVALIDBADGEID123456789"
  val ValidBadgeIdentifier: BadgeIdentifier = BadgeIdentifier(ValidBadgeIdentifierValue)

  val AcceptHeaderValue = "application/vnd.hmrc.1.0+xml"
  val ConnectorContentTypeHeaderValue = s"$XML; charset=UTF-8"

  val TestExtractedHeaders = ExtractedHeadersImpl(ValidBadgeIdentifier, ApiSubscriptionFieldsTestData.clientId, Some(ValidCorrelationIdHeader))
  val TestExtractedHeadersWithoutCorrelationId = TestExtractedHeaders.copy(correlationIdHeader = None)

  lazy val InvalidAcceptHeader: (String, String) = ACCEPT -> JSON
  lazy val InvalidContentTypeJsonHeader: (String, String) = CONTENT_TYPE -> JSON
  lazy val InvalidXClientIdHeader: (String, String) = XClientIdHeaderName -> "This is not a UUID"
  lazy val InvalidXBadgeIdentifier: (String, String) = XBadgeIdentifierHeaderName -> "This is too long and has spaces _"
  lazy val InvalidXCorrelationIdTooLong: (String, String) = XCorrelationIdHeaderName -> "Thisiisslongerrthann34ccharactersssooisttoollong"
  lazy val InvalidXCorrelationIdNonAlphanumeric: (String, String) = XCorrelationIdHeaderName -> "Illegal-char!acter"

  lazy val ValidAcceptHeader: (String, String) = ACCEPT -> AcceptHeaderValue
  lazy val ValidContentTypeHeader: (String, String) = CONTENT_TYPE -> (XML + "; charset=utf-8")
  lazy val ValidXClientIdHeader: (String, String) = XClientIdHeaderName -> ApiSubscriptionFieldsTestData.clientId.value
  lazy val ValidXBadgeIdentifierHeader: (String, String) = XBadgeIdentifierHeaderName -> ValidBadgeIdentifierValue
  lazy val XConversationIdHeader: (String, String) = XConversationIdHeaderName -> ValidConversationId.toString
  lazy val XCorrelationIdHeader: (String, String) = XCorrelationIdHeaderName -> CorrelationIdHeaderValue.toString

  val ValidHeaders = Map(ValidAcceptHeader, ValidContentTypeHeader, ValidXClientIdHeader, ValidXBadgeIdentifierHeader, XConversationIdHeader, XCorrelationIdHeader)

  val CspBearerToken = "CSP-Bearer-Token"

  val ValidateMovementsXsdElementName = "InventoryLinkingImportsValidateMovementResponse"

  val ValidateMovementsXsdLocations = List(
    "/api/conf/1.0/schemas/imports/inventoryLinkingImportValidateMovementResponse.xsd",
    "/api/conf/1.0/schemas/imports/inventoryLinkingImportCommonTypes.xsd"
  )

  val GoodsArrivalXsdElementName= "inventoryLinkingImportsGoodsArrival"

  val GoodsArrivalXsdLocations = List(
    "/api/conf/1.0/schemas/imports/inventoryLinkingImportArriveGoods.xsd",
    "/api/conf/1.0/schemas/imports/inventoryLinkingImportCommonTypes.xsd"
  )

  def elementName(messageType: ImportsMessageType): String = messageType match {
    case _: GoodsArrival => GoodsArrivalXsdElementName
    case _: ValidateMovement => ValidateMovementsXsdElementName
  }

  def otherElementName(messageType: ImportsMessageType): String = messageType match {
    case _: GoodsArrival => ValidateMovementsXsdElementName
    case _: ValidateMovement => GoodsArrivalXsdElementName
  }

  private val year = 2017
  private val monthOfYear = 6
  private val dayOfMonth = 8
  private val hourOfDay = 13
  private val minuteOfHour = 55
  val RequestDateTime = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, DateTimeZone.UTC)

  val RequestDateTimeHttp: String = "2017-06-08T13:55:00Z"
  val BearerToken: String = "token"
  val ValidServiceConfig: ServiceConfig = ServiceConfig("url", Some(BearerToken), "env")

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")

  val ValidValidateMovementRequest: FakeRequest[AnyContentAsXml] = FakeRequest("POST", "/movement-validation")
    .withXmlBody(ValidInventoryLinkingMovementRequestXML)
    .withHeaders(ValidHeaders.toSeq: _*)

  val ValidGoodsArrivalRequest: FakeRequest[AnyContentAsXml] = FakeRequest("POST", "/arrival-notifications")
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
  val stubUniqueIdsService: UniqueIdsService = new UniqueIdsService(mockUuidService) {
    override def conversation: ConversationId = ValidConversationId
    override def correlation: CorrelationId = ValidCorrelationId
  }

  val TestXmlPayload: Elem = <foo>bar</foo>
  val TestFakeRequest: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload)
  val TestConversationIdRequest = ConversationIdRequest(ValidConversationId, TestFakeRequest)
  val TestValidatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestAuthorisedRequest: AuthorisedRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toAuthorisedRequest
  val TestCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toAuthorisedRequest.toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val ValidRequest: FakeRequest[AnyContentAsXml] = TestFakeRequest.withHeaders(ValidHeaders.toSeq: _*)

  def testFakeRequest(badgeIdString: String = ValidBadgeIdentifier.value): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload)

  implicit class FakeRequestOps[R](val fakeRequest: FakeRequest[R]) extends AnyVal {
    def fromCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $CspBearerToken")

    def postTo(endpoint: String): FakeRequest[R] = fakeRequest.copyFakeRequest(method = POST, uri = endpoint)
  }

  val ErrorResponseBadgeIdentifierHeaderMissing: ErrorResponse = errorBadRequest(s"${HeaderConstants.XBadgeIdentifier} header is missing or invalid")
  val ErrorResponseCorrelationIdHeaderMissing: ErrorResponse = errorBadRequest(s"${HeaderConstants.XCorrelationId} header is missing or invalid")
}
