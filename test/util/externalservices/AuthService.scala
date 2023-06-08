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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsMessageType, ValidateMovement}
import util.TestData
import util.TestData.{GoodsArrivalAuthPredicate, ValidateMovementAuthPredicate}

trait AuthService {

  val authUrl = "/auth/authorise"
  private val authUrlMatcher = urlEqualTo(authUrl)

  private def cspAuthorisationPredicate(enrolment: Enrolment) = enrolment and AuthProviders(PrivilegedApplication)

  private def bearerTokenMatcher(bearerToken: String)= equalTo("Bearer " + bearerToken)

  private def authRequestJson(predicate: Predicate, retrievals: Retrieval[_]*): String = {
    val predicateJsArray = predicate.toJson match {
      case arr: JsArray => arr
      case other => Json.arr(other)
    }
    val js =
      s"""
         |{
         |  "authorise": $predicateJsArray,
         |  "retrieve": [${retrievals.flatMap(_.propertyNames).map(Json.toJson(_)).mkString(",")}]
         |}
    """.stripMargin
    js
  }

  def authServiceAuthorisesCSP(messageType: ImportsMessageType, bearerToken: String = TestData.CspBearerToken): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(predicateForMessageType(messageType))))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody("{}")
      )
    )
  }

  def authServiceUnAuthorisesScopeForCSP(messageType: ImportsMessageType, bearerToken: String = TestData.CspBearerToken): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(predicateForMessageType(messageType))))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
      .willReturn(
        aResponse()
          .withStatus(Status.UNAUTHORIZED)
          .withHeader(WWW_AUTHENTICATE, """MDTP detail="InsufficientEnrolments"""")
      )
    )
  }

  def verifyAuthServiceCalledForCsp(enrolment: Enrolment, bearerToken: String = TestData.CspBearerToken): Unit = {
    verify(1, postRequestedFor(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(cspAuthorisationPredicate(enrolment))))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
    )
  }

  private def predicateForMessageType(messageType: ImportsMessageType) = messageType match {
    case _: GoodsArrival => GoodsArrivalAuthPredicate
    case _: ValidateMovement => ValidateMovementAuthPredicate
    case _: ImportsMessageType => EmptyPredicate
  }

}
