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
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.{ImportsConnector, OutgoingRequest, OutgoingRequestBuilder}
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsMessageType, ValidateMovement}
import uk.gov.hmrc.customs.inventorylinking.imports.services._
import uk.gov.hmrc.http.HttpResponse
import util.TestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageSenderSpec extends WordSpecLike with Matchers with MockitoSugar with TableDrivenPropertyChecks {

  trait SetUp {
    protected val importsMessageType: ImportsMessageType

    val httpResponse: AnyRef with HttpResponse = HttpResponse(ACCEPTED)
    private val outgoingRequestBuilder = mock[OutgoingRequestBuilder]
    lazy val goodsArrivalXmlValidationService: GoodsArrivalXmlValidationService = mock[GoodsArrivalXmlValidationService]
    lazy val validateMovementXmlValidationService: ValidateMovementXmlValidationService = mock[ValidateMovementXmlValidationService]
    val connector: ImportsConnector = mock[ImportsConnector]
    val headers: Map[String, String] = Map("header" -> "value")
    val sender: MessageSender = new MessageSender(outgoingRequestBuilder, goodsArrivalXmlValidationService, validateMovementXmlValidationService, connector)
    val outgoingRequest = OutgoingRequest(serviceConfig, body, requestInfo)

    protected def service: XmlValidationService = importsMessageType match {
      case GoodsArrival => goodsArrivalXmlValidationService
      case ValidateMovement => validateMovementXmlValidationService
    }

    when(outgoingRequestBuilder.build(ValidateMovement, requestInfo, headers, body)).thenReturn(outgoingRequest)
  }

  val messageTypes = Table(
    "Message type",
    GoodsArrival,
    ValidateMovement
  )

  forAll(messageTypes){(messageType) =>
    s"$messageType send" when {
      "message is valid" should {
        "return the result from the connector" in new SetUp {
          val importsMessageType: ImportsMessageType = messageType
          when(connector.post(outgoingRequest)).thenReturn(Future.successful(httpResponse))
          when(service.validate(body)).thenReturn(Future.successful(()))

          private val result = sender.send(messageType, body, requestInfo, headers)

          result.foreach(r => r shouldBe httpResponse)
        }
      }

      "message is invalid" should {
        "return failed future" in new SetUp {
          val importsMessageType: ImportsMessageType = messageType
          when(service.validate(body)).thenReturn(Future.failed(emulatedServiceFailure))

          private val result = sender.send(messageType, body, requestInfo, headers)

          result.foreach(r => r shouldBe httpResponse)
        }
      }
    }
  }

}
