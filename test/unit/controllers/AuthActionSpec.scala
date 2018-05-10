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

package unit.controllers

import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders.{AuthAction, ValidateMovementAuthAction}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.ValidateMovement
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.play.test.UnitSpec
import util.AuthConnectorStubbing
import util.TestData.{TestExtractedHeaders, conversationId, testFakeRequest}

class AuthActionSpec extends UnitSpec with MockitoSugar {

  private lazy val validatedHeadersRequest =
    ConversationIdRequest(conversationId, testFakeRequest()).toValidatedHeadersRequest(TestExtractedHeaders)

  trait SetUp extends AuthConnectorStubbing {
    val mockImportsLogger: ImportsLogger = mock[ImportsLogger]
    val authAction: AuthAction = new ValidateMovementAuthAction(mockAuthConnector, new ValidateMovement(), mockImportsLogger)
  }

//TODO add GA test
  "CspAuthAction" should {
    "Return Right of AuthorisedRequest CSP when authorised by auth API" in new SetUp {
      authoriseCsp()

      private val actual = await(authAction.refine(validatedHeadersRequest))
      actual shouldBe Right(validatedHeadersRequest.toAuthorisedRequest)
      verifyCspAuthorisationCalled(1)
    }


//    "propagate exceptional errors in CSP auth API call" in new SetUp {
//      authoriseCspError()
//
//      private val caught = intercept[Throwable](await(authAction.Authenticate(mockAuthConnector, ValidateMovement).refine(TestValidatedHeadersRequestNoBadge)))
//
//      caught shouldBe emulatedServiceFailure
//      verifyCspAuthorisationCalled(1)
//    }

  }

}
