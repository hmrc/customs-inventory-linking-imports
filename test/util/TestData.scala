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

package util

import java.util.UUID
import com.google.inject.AbstractModule

import java.time._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.MimeTypes.{JSON, XML}
import play.api.inject.guice.GuiceableModule
import play.api.mvc.AnyContentAsXml
import play.api.mvc.request.RequestTarget
import play.api.test.FakeRequest
import play.api.test.Helpers.{ACCEPT, CONTENT_TYPE, POST}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{AuthProviders, Enrolment}
import uk.gov.hmrc.customs.inventorylinking.imports.config.ServiceConfig
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders._
import uk.gov.hmrc.customs.inventorylinking.imports.services.{DateTimeService, UniqueIdsService, UuidService}
import util.CustomsMetricsTestData.EventStart
import util.XMLTestData.{ValidInventoryLinkingGoodsArrivalRequestXML, ValidInventoryLinkingMovementRequestXML, validWrappedGoodsArrivalXml, validWrappedValidateMovementXml}

import java.time.format.DateTimeFormatter
import scala.xml.Elem

object TestData {

  val XBadgeIdentifierHeaderName = "X-Badge-Identifier"
  val XClientIdHeaderName = "X-Client-ID"
  val XConversationIdHeaderName = "X-Conversation-ID"
  val XCorrelationIdHeaderName = "X-Correlation-ID"
  val XSubmitterIdentifierHeaderName = "X-Submitter-Identifier"

  val ConversationIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  val ConversationIdUuid: UUID = UUID.fromString(ConversationIdValue)
  val ValidConversationId: ConversationId = ConversationId(ConversationIdUuid)

  val CorrelationIdValue = "e61f8eee-812c-4b8f-b193-06aedc60dca2"
  val CorrelationIdUuid: UUID = UUID.fromString(CorrelationIdValue)
  val ValidCorrelationId = CorrelationId(CorrelationIdUuid)

  val CorrelationIdHeaderValue = "abCabC123u"
  val ValidCorrelationIdHeader = CorrelationIdHeader(CorrelationIdHeaderValue)

  val SubmitterIdentifierHeaderValue = "xsubmitterid123"
  val ValidSubmitterIdentifierHeader = SubmitterIdentifier(SubmitterIdentifierHeaderValue)

  val AuthenticatedEoriValue = "RASHADMUGHAL"
  val ValidAuthenticatedEori = AuthenticatedEori(AuthenticatedEoriValue)

  val EntryNumberValue = "23GB6BN2FXCDS58A00"
  val ValidEntryNumber = EntryNumber(EntryNumberValue)

  val ValidBadgeIdentifierValue = "BADGEID123"
  val InvalidBadgeIdentifierValue = "INVALIDBADGEID123456789"
  val ValidBadgeIdentifier: BadgeIdentifier = BadgeIdentifier(ValidBadgeIdentifierValue)

  val AcceptHeaderValueV1 = "application/vnd.hmrc.1.0+xml"
  val AcceptHeaderValueV2 = "application/vnd.hmrc.2.0+xml"
  val AcceptHeaderV1: (String, String) = ACCEPT -> AcceptHeaderValueV1
  val AcceptHeaderV2: (String, String) = ACCEPT -> AcceptHeaderValueV2

  val ConnectorContentTypeHeaderValue = s"$XML; charset=UTF-8"

  val TestExtractedHeadersV1 = ExtractedHeadersImpl(VersionOne, Some(ValidBadgeIdentifier), ApiSubscriptionFieldsTestData.clientId, Some(ValidCorrelationIdHeader), Some(ValidSubmitterIdentifierHeader))
  val TestExtractedHeadersV2 = ExtractedHeadersImpl(VersionTwo, Some(ValidBadgeIdentifier), ApiSubscriptionFieldsTestData.clientId, Some(ValidCorrelationIdHeader), Some(ValidSubmitterIdentifierHeader))
  val TestExtractedHeadersWithoutCorrelationId = TestExtractedHeadersV1.copy(maybeCorrelationIdHeader = None)
  val TestExtractedHeadersWithoutCorrelationIdOrBadgeId = TestExtractedHeadersWithoutCorrelationId.copy(maybeBadgeIdentifier = None)
  val TestExtractedHeadersWithoutCorrelationIdOrSubmitterId = TestExtractedHeadersWithoutCorrelationId.copy(maybeSubmitterIdentifier = None)
  val TestExtractedHeadersWithoutCorrelationIdOrSubmitterIdOrBadgeId = TestExtractedHeadersWithoutCorrelationId.copy(maybeSubmitterIdentifier = None, maybeBadgeIdentifier = None)

  lazy val InvalidAcceptHeader: (String, String) = ACCEPT -> JSON
  lazy val InvalidContentTypeJsonHeader: (String, String) = CONTENT_TYPE -> JSON
  lazy val InvalidXClientIdHeader: (String, String) = XClientIdHeaderName -> "This is not a UUID"
  lazy val InvalidXBadgeIdentifier: (String, String) = XBadgeIdentifierHeaderName -> "This is too long and has spaces _"
  lazy val InvalidXCorrelationIdTooLong: (String, String) = XCorrelationIdHeaderName -> "Thisiisslongerrthann34ccharactersssooisttoollong"
  lazy val InvalidXCorrelationIdNonAlphanumeric: (String, String) = XCorrelationIdHeaderName -> "Illegal-char!acter"
  lazy val InvalidXSubmitterIdentifierNonAlphanumeric: (String, String) = XSubmitterIdentifierHeaderName -> "Illegal-char!acter"
  lazy val InvalidXSubmitterIdentifierLongerThan17: (String, String) = XSubmitterIdentifierHeaderName -> "12345678901234567890"
  lazy val InvalidXSubmitterIdentifierEmpty: (String, String) = XSubmitterIdentifierHeaderName -> ""

  lazy val ValidAcceptHeader: (String, String) = ACCEPT -> AcceptHeaderValueV1
  lazy val ValidContentTypeHeader: (String, String) = CONTENT_TYPE -> (XML + "; charset=utf-8")
  lazy val ValidXClientIdHeader: (String, String) = XClientIdHeaderName -> ApiSubscriptionFieldsTestData.clientId.value
  lazy val ValidXBadgeIdentifierHeader: (String, String) = XBadgeIdentifierHeaderName -> ValidBadgeIdentifierValue
  lazy val XConversationIdHeader: (String, String) = XConversationIdHeaderName -> ValidConversationId.toString
  lazy val XCorrelationIdHeader: (String, String) = XCorrelationIdHeaderName -> CorrelationIdHeaderValue.toString
  lazy val XSubmitterIdentifierHeader: (String, String) = XSubmitterIdentifierHeaderName -> SubmitterIdentifierHeaderValue.toString

  val ValidHeaders = Map(ValidAcceptHeader, ValidContentTypeHeader, ValidXClientIdHeader, ValidXBadgeIdentifierHeader, XConversationIdHeader, XCorrelationIdHeader, XSubmitterIdentifierHeader)
  val ValidHeadersNoCorrelationId = Map(ValidAcceptHeader, ValidContentTypeHeader, ValidXClientIdHeader, ValidXBadgeIdentifierHeader, XConversationIdHeader, XSubmitterIdentifierHeader)

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
    case _: ImportsMessageType => ""
  }

  def otherElementName(messageType: ImportsMessageType): String = messageType match {
    case _: GoodsArrival => ValidateMovementsXsdElementName
    case _: ValidateMovement => GoodsArrivalXsdElementName
    case _: ImportsMessageType => ""
  }

  val RequestDateTime = LocalDateTime.now()

  val requestIsoFormatDate: DateTimeFormatter = new DateTimeService().isoFormatNoMillis
  val RequestDateTimeHttp: String = LocalDateTime.now().atOffset(ZoneOffset.UTC).format(requestIsoFormatDate)
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
    override def configure(): Unit = {
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
  val TestFakeRequestV1: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(AcceptHeaderV1)
  val TestFakeRequestV2: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(AcceptHeaderV2)
  val TestConversationIdRequestV1: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(ValidConversationId, EventStart, TestFakeRequestV1)
  val TestConversationIdRequestV2: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(ValidConversationId, EventStart, TestFakeRequestV2)
  val TestApiVersionRequestV1: ApiVersionRequest[AnyContentAsXml] = ApiVersionRequest(ValidConversationId, EventStart, VersionOne, TestFakeRequestV1)
  val TestApiVersionRequestV2: ApiVersionRequest[AnyContentAsXml] = ApiVersionRequest(ValidConversationId, EventStart, VersionTwo, TestFakeRequestV2)
  val TestValidatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequestV1.toValidatedHeadersRequest(TestExtractedHeadersV1)
  val TestValidatedHeadersRequestV2: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequestV1.toValidatedHeadersRequest(TestExtractedHeadersV2)
  val TestValidatedHeadersNoIdsRequest: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequestV1.toValidatedHeadersRequest(TestExtractedHeadersWithoutCorrelationIdOrSubmitterIdOrBadgeId)
  val TestAuthorisedRequest: AuthorisedRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toAuthorisedRequest
  val TestCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toAuthorisedRequest.toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestCspValidatedPayloadRequestWithValidPayload: ValidatedPayloadRequest[AnyContentAsXml] = ValidatedPayloadRequest(None,ValidConversationId,EventStart,VersionOne, None, None,ClientId("124353"), xmlBody = ValidInventoryLinkingMovementRequestXML, TestFakeRequestV1)
  val TestCspValidatedPayloadRequestV2: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequestV2.toAuthorisedRequest.toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestCspValidatedPayloadRequestNoIds: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersNoIdsRequest.toAuthorisedRequest.toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val ValidRequest: FakeRequest[AnyContentAsXml] = TestFakeRequestV1.withHeaders(ValidHeaders.toSeq: _*)

  def testFakeRequest(badgeIdString: String = ValidBadgeIdentifier.value): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload)

  implicit class FakeRequestOps[R](val fakeRequest: FakeRequest[R]) extends AnyVal {
    def fromCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $CspBearerToken")

    def postTo(endpoint: String): FakeRequest[R] = fakeRequest.withMethod(POST)
      .withTarget(RequestTarget(path = endpoint, uriString = fakeRequest.uri, queryString = fakeRequest.queryString))
  }

  val ErrorResponseBadgeIdentifierHeaderMissing: ErrorResponse = errorBadRequest(s"${HeaderConstants.XBadgeIdentifier} header is invalid")
  val ErrorResponseCorrelationIdHeaderMissing: ErrorResponse = errorBadRequest(s"${HeaderConstants.XCorrelationId} header is missing or invalid")
  val ErrorResponseSubmitterIdentifierHeaderMissing: ErrorResponse = errorBadRequest(s"${HeaderConstants.XSubmitterIdentifier} header is invalid")
}
