/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.controllers.actionbuilders

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorInternalServerError
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders.{GoodsArrivalAuthAction, ValidateMovementAuthAction}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, HeaderConstants, ValidateMovement}
import uk.gov.hmrc.play.test.UnitSpec
import util.AuthConnectorStubbing
import util.TestData.{ConversationIdValue, TestExtractedHeadersV1, ValidConversationId, testFakeRequest}

class AuthActionSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks with BeforeAndAfterEach {

  private lazy val validatedHeadersRequest =
    ConversationIdRequest(ValidConversationId, testFakeRequest()).toValidatedHeadersRequest(TestExtractedHeadersV1)
  private val mockAuthenticationConnector: AuthConnector = mock[AuthConnector]
  private val mockImportsLogger: ImportsLogger = mock[ImportsLogger]
  private implicit val ec = Helpers.stubControllerComponents().executionContext
  
  trait SetUp extends AuthConnectorStubbing {
    override val mockAuthConnector: AuthConnector = mockAuthenticationConnector
  }

  override protected def beforeEach(): Unit = {
    reset(mockAuthenticationConnector)
  }

  "CspAuthAction" can {
    "for Goods Arrival" should {
      val enrolment = Enrolment("write:customs-il-imports-arrival-notifications")
      val authAction = new GoodsArrivalAuthAction(mockAuthenticationConnector, new GoodsArrival(), mockImportsLogger)

      "return AuthorisedRequest for CSP when authorised by auth API" in new SetUp {
        authoriseCsp(enrolment)

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Right(validatedHeadersRequest.toAuthorisedRequest)
        verifyCspAuthorisationCalled(enrolment, 1)
      }

      "return ErrorResponse with ConversationId when not authorised by auth API" in new SetUp {
        authoriseCspError(enrolment)

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(HeaderConstants.XConversationId -> ConversationIdValue))
        verifyCspAuthorisationCalled(enrolment, 1)
      }
    }

    "for Validate Movement" should {
      val enrolment = Enrolment("write:customs-il-imports-movement-validation")
      val authAction = new ValidateMovementAuthAction(mockAuthenticationConnector, new ValidateMovement(), mockImportsLogger)

      "return AuthorisedRequest for CSP when authorised by auth API" in new SetUp {
        authoriseCsp(enrolment)

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Right(validatedHeadersRequest.toAuthorisedRequest)
        verifyCspAuthorisationCalled(enrolment, 1)
      }

      "return ErrorResponse with ConversationId when not authorised by auth API" in new SetUp {
        authoriseCspError(enrolment)

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(HeaderConstants.XConversationId -> ConversationIdValue))
        verifyCspAuthorisationCalled(enrolment, 1)
      }
    }
  }

}
