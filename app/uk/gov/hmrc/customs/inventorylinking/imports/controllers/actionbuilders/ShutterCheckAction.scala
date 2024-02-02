/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.mvc.{ActionRefiner, _}
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ErrorResponse
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.ErrorResponse.ErrorAcceptHeaderInvalid
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants.{Version1AcceptHeaderValue, Version2AcceptHeaderValue}
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper.AddConversationId
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ApiVersion, VersionOne, VersionTwo}
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{ApiVersionRequest, ConversationIdRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.services.ImportsConfigService

import scala.concurrent.{ExecutionContext, Future}

/** Action builder that validates headers.
  * <ol>
  * <li/>Input - `ConversationIdRequest`
  * <li/>Output - `ApiVersionRequest`
  * <li/>Error - If Accept header is missing or invalid then return a 406. If requested version is shuttered then return a 503. This terminates the action builder pipeline.
  * </ol>
  */
@Singleton
class ShutterCheckAction @Inject()(logger: ImportsLogger,
                                   config: ImportsConfigService)
                                  (implicit ec: ExecutionContext)
  extends ActionRefiner[ConversationIdRequest, ApiVersionRequest] {
    actionName =>

  private val errorResponseVersionShuttered: Result = ErrorResponse(SERVICE_UNAVAILABLE, "SERVER_ERROR", "Service unavailable").XmlResult

  protected val versionsByAcceptHeader: Map[String, ApiVersion] = Map(
      Version1AcceptHeaderValue -> VersionOne,
      Version2AcceptHeaderValue -> VersionTwo
    )

  protected lazy val versionShuttered: Map[ApiVersion, Boolean] = Map(
    VersionOne -> config.importsShutterConfig.v1Shuttered.getOrElse(false),
    VersionTwo -> config.importsShutterConfig.v2Shuttered.getOrElse(false)
  )

    override def executionContext: ExecutionContext = ec

    override def refine[A](cir: ConversationIdRequest[A]): Future[Either[Result, ApiVersionRequest[A]]] = Future.successful {
      implicit val id: ConversationIdRequest[A] = cir
      val acceptErrorResult = Left(ErrorAcceptHeaderInvalid.XmlResult.withConversationId)
      val serviceUnavailableResult = Left(errorResponseVersionShuttered)

      cir.request.headers.get(ACCEPT) match {
        case None =>
          logger.error(s"Error - header '$ACCEPT' not present")
          acceptErrorResult
        case Some(v) =>
          if (!versionsByAcceptHeader.keySet.contains(v)) {
            logger.error(s"Error - header '$ACCEPT' value '$v' is not valid")
            acceptErrorResult
          } else {
            val apiVersion: ApiVersion = versionsByAcceptHeader(v)
            if (versionShuttered(apiVersion)) {
              logger.warn(s"version ${apiVersion.toString} requested but is shuttered")
              serviceUnavailableResult
            } else {
              logger.debug(s"$ACCEPT header passed validation with: $apiVersion")
              Right(ApiVersionRequest(cir.conversationId, cir.start, apiVersion, cir.request))
            }
          }
     }
  }
}
