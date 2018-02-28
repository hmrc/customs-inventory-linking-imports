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
import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ImportsConnector, OutgoingRequest, OutgoingRequestBuilder}
import uk.gov.hmrc.customs.inventorylinking.imports.model.ValidateMovement
import uk.gov.hmrc.customs.inventorylinking.imports.services.{MessageSender, XmlValidationService}
import uk.gov.hmrc.http.HttpResponse
import util.TestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageSenderSpec extends WordSpecLike with Matchers with MockitoSugar {

  trait Setup {
    val httpResponse: AnyRef with HttpResponse = HttpResponse(ACCEPTED)
    private val outgoingRequestBuilder = mock[OutgoingRequestBuilder]
    val xmlValidationService: XmlValidationService = mock[XmlValidationService]
    val connector: ImportsConnector = mock[ImportsConnector]
    val headers: Map[String, String] = Map("header" -> "value")
    val sender: MessageSender = new MessageSender(outgoingRequestBuilder, xmlValidationService, connector)

    val outgoingRequest = OutgoingRequest(serviceConfig, body, requestInfo)
    when(outgoingRequestBuilder.build(ValidateMovement, requestInfo, headers, body)).thenReturn(outgoingRequest)

    when(xmlValidationService.validate(body)).thenReturn(Future.successful(()))
  }

  "send" when {
    "message is valid" should {
      "return the result from the connector" in new Setup {
        when(connector.post(outgoingRequest)).thenReturn(Future.successful(httpResponse))

        val result = sender.send(ValidateMovement, body, requestInfo, headers)

        result.foreach(r => r shouldBe httpResponse)
      }
    }

    "message is invalid" should {
      "return failed future" in new Setup {
        when(xmlValidationService.validate(body)).thenReturn(Future.failed(emulatedServiceFailure))

        val result = sender.send(ValidateMovement, body, requestInfo, headers)

        result.foreach(r => r shouldBe httpResponse)
      }
    }
  }
}
