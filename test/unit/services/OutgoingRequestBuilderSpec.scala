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
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{OutgoingRequest, OutgoingRequestBuilder}
import uk.gov.hmrc.customs.inventorylinking.imports.model.ValidateMovement
import uk.gov.hmrc.customs.inventorylinking.imports.xml.PayloadDecorator
import util.TestData.Headers._
import util.TestData._

class OutgoingRequestBuilderSpec extends WordSpecLike with Matchers with MockitoSugar {

  trait Setup {
    val serviceConfigProvider: ServiceConfigProvider = mock[ServiceConfigProvider]
    val headers: Map[String, String] = Map(xClientId, xBadgeIdentifier)
    val decorator: PayloadDecorator = mock[PayloadDecorator]
    val builder: OutgoingRequestBuilder = new OutgoingRequestBuilder(serviceConfigProvider, decorator)

    when(decorator.wrap(body, requestInfo, clientId, badgeIdentifier, "InventoryLinkingImportsInboundValidateMovementResponse")).thenReturn(decoratedBody)
    when(serviceConfigProvider.getConfig("validatemovement")).thenReturn(serviceConfig)

    val result: OutgoingRequest = builder.build(requestInfo, headers, body, ValidateMovement)
  }

  "build" should {
    "return outgoing request with config" in new Setup {
      result.service shouldBe serviceConfig
    }

    "return outgoing request with decorated body" in new Setup {
      result.body shouldBe decoratedBody
    }

    "return outgoing request with request info" in new Setup {
      result.requestInfo shouldBe requestInfo
    }
  }
}
