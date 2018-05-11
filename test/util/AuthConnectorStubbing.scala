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

package util

import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

trait AuthConnectorStubbing extends UnitSpec with MockitoSugar {
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val apiScope = "write:customs-il-imports-arrival-notifications"
  private val cspAuthPredicate = Enrolment(apiScope) and AuthProviders(PrivilegedApplication)

  def authoriseCsp(): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(())
  }

  def authoriseCspError(): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(TestData.emulatedServiceFailure))
  }

  def unauthoriseCsp(authException: AuthorisationException = new InsufficientEnrolments): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(authException))
  }

  def verifyCspAuthorisationCalled(numberOfTimes: Int): Future[Unit] = {
    verify(mockAuthConnector, times(numberOfTimes))
      .authorise(ameq(cspAuthPredicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext])
  }

}
