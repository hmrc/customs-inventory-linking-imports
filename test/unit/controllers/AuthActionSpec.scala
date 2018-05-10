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

import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, Enrolment}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders.{GoodsArrivalAuthAction, ValidateMovementAuthAction}
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ValidateMovement}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData
import util.TestData.{TestExtractedHeaders, testFakeRequest, emulatedServiceFailure, ValidConversationId}

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  private lazy val validatedHeadersRequest =
    ConversationIdRequest(ValidConversationId, testFakeRequest()).toValidatedHeadersRequest(TestExtractedHeaders)
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockImportsLogger: ImportsLogger = mock[ImportsLogger]

  private val authActionTypes = Table(
    ("Message Name", "Enrolment", "Auth Action"),
    ("Goods Arrival", Enrolment("write:customs-il-imports-arrival-notifications"), new GoodsArrivalAuthAction(mockAuthConnector, new GoodsArrival(), mockImportsLogger)),
    ("Validate Movement", Enrolment("write:customs-il-imports-movement-validation"), new ValidateMovementAuthAction(mockAuthConnector, new ValidateMovement(), mockImportsLogger))
  )

  forAll(authActionTypes) { (messageName, enrolment, authAction) =>
    "CspAuthAction" should {
      s"Return Right of $messageName AuthorisedRequest CSP when authorised by auth API" in {
        authoriseCsp(enrolment)

        val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Right(validatedHeadersRequest.toAuthorisedRequest)
        verifyCspAuthorisationCalled(enrolment, 1)
      }

//      s"propagate exceptional errors in $messageName CSP auth API call" in {
//        authoriseCspError(Enrolment(""))
//
//        val caught = intercept[Throwable](await(authAction.refine(validatedHeadersRequest)))
//
//        caught shouldBe emulatedServiceFailure
//        verifyCspAuthorisationCalled(enrolment, 1)
//      }

    }
  }

  def authoriseCsp(apiScope: Enrolment): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate(apiScope)), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(())
  }

  def cspAuthPredicate(apiScope: Enrolment): Predicate = {
    apiScope and AuthProviders(PrivilegedApplication)
  }

  def verifyCspAuthorisationCalled(apiScope: Enrolment, numberOfTimes: Int): Future[Unit] = {
    verify(mockAuthConnector, times(numberOfTimes))
      .authorise(ameq(cspAuthPredicate(apiScope)), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext])
  }

  def authoriseCspError(apiScope: Enrolment): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate(apiScope)), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(TestData.emulatedServiceFailure))
  }

}
