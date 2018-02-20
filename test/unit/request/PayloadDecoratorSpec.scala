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

package unit.request

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.inventorylinking.imports.request.{PayloadDecorator, RequestInfo}
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.NodeSeq

class PayloadDecoratorSpec extends UnitSpec with MockitoSugar {

  private val xml: NodeSeq = <node1></node1>

  private val clientId = "clientID"
  private val conversationId = UUID.fromString("3dbc19cb-94a2-45f1-ab54-30ff328ce9cb")
  private val correlationId = UUID.fromString("403f287e-376f-4aca-9fb9-5d96b5507cd2")
  private val badgeIdentifier = "badgeIdentifier"

  private val year = 2017
  private val monthOfYear = 6
  private val dayOfMonth = 8
  private val hourOfDay = 13
  private val minuteOfHour = 55
  private val secondOfMinute = 0
  private val millisOfSecond = 0
  private val dateTime = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond, DateTimeZone.UTC)
  private val requestInfo = RequestInfo(conversationId, correlationId, dateTime)

  private val payloadWrapper = new PayloadDecorator

  private def wrapPayload() = payloadWrapper.wrap(xml, requestInfo, clientId, badgeIdentifier)

  "WcoDmsPayloadWrapper" should {
    "wrap passed XML in DMS wrapper" in {
      val result = wrapPayload()

      val reqDet = result \\ "requestDetail"
      reqDet.head.child.contains(<node1 />) shouldBe true
    }

    "set the dateTimeStamp in the wrapper" in {
      val result = wrapPayload()

      val rd = result \\ "dateTimeStamp"

      rd.head.text shouldBe "2017-06-08T13:55:00Z"
    }

    "set the conversationId" in {
      val result = wrapPayload()

      val rd = result \\ "conversationID"

      rd.head.text shouldBe conversationId.toString
    }

    "set the clientId" in {
      val result = wrapPayload()

      val rd = result \\ "clientID"

      rd.head.text shouldBe clientId
    }

    "set the correlationID" in {
      val result = wrapPayload()

      val rd = result \\ "correlationID"

      rd.head.text shouldBe correlationId.toString
    }

    "set the badgeIdentifier" in {
      val result = wrapPayload()

      val rd = result \\ "badgeIdentifier"

      rd.head.text shouldBe badgeIdentifier
    }
  }
}
