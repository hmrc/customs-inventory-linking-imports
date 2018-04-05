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

package unit.services

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{OutgoingRequest, OutgoingRequestBuilder}
import uk.gov.hmrc.customs.inventorylinking.imports.model.{HeaderMap, RequestData, ValidateMovement, ValidatedRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator
import util.ApiSubscriptionFieldsTestData.FieldsId
import util.TestData._

class OutgoingRequestBuilderSpec extends WordSpecLike with Matchers with MockitoSugar {

  trait Setup {
    val serviceConfigProvider: ServiceConfigProvider = mock[ServiceConfigProvider]
    val headers: HeaderMap = Map(ValidXClientIdHeader, ValidXBadgeIdentifierHeader)
    val decorator: PayloadDecorator = mock[PayloadDecorator]
    val builder: OutgoingRequestBuilder = new OutgoingRequestBuilder(serviceConfigProvider, decorator)

    lazy val mockRequestData = mock[RequestData]
    lazy val mockRequest = mock[Request[AnyContent]]
    val mockBody: AnyContent = mock[AnyContent]

    val mockValidatedRequest = ValidatedRequest[AnyContent](mockRequestData, mockRequest)

    when(mockRequest.body).thenReturn(mockBody)
    when(mockBody.asXml).thenReturn(Some(outgoingBody))
    when(mockRequestData.badgeIdentifier).thenReturn(XBadgeIdentifierHeaderValueAsString)
    when(decorator.wrap(mockValidatedRequest, FieldsId, "InventoryLinkingImportsInboundValidateMovementResponse")).thenReturn(decoratedBody)
    when(serviceConfigProvider.getConfig("validatemovement")).thenReturn(serviceConfig)

    val result: OutgoingRequest = builder.build(ValidateMovement, mockValidatedRequest, FieldsId)
  }

  "build" should {
    "return outgoing request with config" in new Setup {
      result.service shouldBe serviceConfig
    }

    "return outgoing request with decorated body" in new Setup {
      result.outgoingBody shouldBe decoratedBody
    }
  }
}
