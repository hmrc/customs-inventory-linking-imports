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

package unit.logging

import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.inventorylinking.imports.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{AuthorisedRequest, ConversationIdRequest}
import util.CustomsMetricsTestData.EventStart
import util.UnitSpec
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData.{TestExtractedHeadersV1, TestXmlPayload, ValidConversationId, ValidEntryNumber, emulatedServiceFailure}

class ImportsLoggerSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    val mockCdsLogger: CdsLogger = mock[CdsLogger]
    val logger = new ImportsLogger(mockCdsLogger)
    implicit val implicitVpr: AuthorisedRequest[AnyContentAsXml] = ConversationIdRequest(ValidConversationId, EventStart, FakeRequest()
      .withXmlBody(TestXmlPayload).withHeaders("Content-Type" -> "Some-Content-Type"))
      .toValidatedHeadersRequest(TestExtractedHeadersV1)
      .toAuthorisedRequest
  }

  "ImportsLogger" should {
    "debug(s: => String)" in new SetUp {
      logger.debug("msg")

      PassByNameVerifier(mockCdsLogger, "debug")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][badgeIdentifier=BADGEID123][submitterIdentifier=xsubmitterid123] msg")
        .verify()
    }
    "debug(s: => String, e: => Throwable)" in new SetUp {
      logger.debug("msg", emulatedServiceFailure)

      PassByNameVerifier(mockCdsLogger, "debug")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][badgeIdentifier=BADGEID123][submitterIdentifier=xsubmitterid123] msg")
        .withByNameParam(emulatedServiceFailure)
        .verify()
    }
    "debugFull(s: => String)" in new SetUp {
      logger.debugFull("msg")

      PassByNameVerifier(mockCdsLogger, "debug")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d] msg headers=TreeMap(Content-Type -> Some-Content-Type)")
        .verify()
    }
    "info(s: => String)" in new SetUp {
      logger.info("msg")

      PassByNameVerifier(mockCdsLogger, "info")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][badgeIdentifier=BADGEID123][submitterIdentifier=xsubmitterid123] msg")
        .verify()
    }
    "infoEn(s: => String, en:Option[EntryNumber])" in new SetUp {
      logger.infoEn("msg", Some(ValidEntryNumber))

      PassByNameVerifier(mockCdsLogger, "info")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][badgeIdentifier=BADGEID123][submitterIdentifier=xsubmitterid123][entryNumber=23GB6BN2FXCDS58A00] msg")
        .verify()
    }
    "info(s: => String, en:Option[EntryNumber] = None)" in new SetUp {
      logger.infoEn("msg", None)

      PassByNameVerifier(mockCdsLogger, "info")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][badgeIdentifier=BADGEID123][submitterIdentifier=xsubmitterid123] msg")
        .verify()
    }

    "warn(s: => String)" in new SetUp {
      logger.warn("msg")

      PassByNameVerifier(mockCdsLogger, "warn")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][badgeIdentifier=BADGEID123][submitterIdentifier=xsubmitterid123] msg")
        .verify()
    }
    "error(s: => String, e: => Throwable)" in new SetUp {
      logger.error("msg", emulatedServiceFailure)

      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][badgeIdentifier=BADGEID123][submitterIdentifier=xsubmitterid123] msg")
        .withByNameParam(emulatedServiceFailure)
        .verify()
    }
    "error(s: => String)" in new SetUp {
      logger.error("msg")

      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][badgeIdentifier=BADGEID123][submitterIdentifier=xsubmitterid123] msg")
        .verify()
    }
    "errorWithoutRequestContext(s: => String)" in new SetUp {
      logger.errorWithoutRequestContext("msg")

      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam("msg")
        .verify()
    }
  }
}
