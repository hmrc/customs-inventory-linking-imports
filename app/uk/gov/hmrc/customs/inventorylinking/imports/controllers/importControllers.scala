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

package uk.gov.hmrc.customs.inventorylinking.imports.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.MimeTypes
import play.api.mvc.{Action, AnyContent, _}
import uk.gov.hmrc.customs.inventorylinking.imports.connectors.CustomsMetricsConnector
import uk.gov.hmrc.customs.inventorylinking.imports.controllers.actionbuilders._
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.inventorylinking.imports.services.MessageSender
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class Common @Inject() (val conversationIdAction: ConversationIdAction,
                        val shutterCheckAction: ShutterCheckAction,
                        val messageSender: MessageSender,
                        val cc: ControllerComponents,
                        val metricsConnector: CustomsMetricsConnector,
                        val logger: ImportsLogger)

abstract class ImportController(val common: Common,
                                val importsMessageType: ImportsMessageType,
                                val authAction: AuthAction,
                                val payloadValidationAction: PayloadValidationAction,
                                val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction)
                               (implicit ex: ExecutionContext)
  extends BackendController(common.cc) {

  def process(): Action[AnyContent] =  (
    Action andThen
      common.conversationIdAction andThen
      common.shutterCheckAction andThen
      validateAndExtractHeadersAction andThen
      authAction andThen
      payloadValidationAction
    )
    .async(bodyParser = xmlOrEmptyBody) {

      implicit vpr: ValidatedPayloadRequest[AnyContent] =>
        common.messageSender.send(importsMessageType) map {
        case Right(_) =>
          common.logger.info("Inventory linking imports request processed successfully")
          common.metricsConnector.post(CustomsMetricsRequest(
            "ILI", vpr.conversationId, vpr.start, common.conversationIdAction.timeService.zonedDateTimeUtc))
          Accepted.as(MimeTypes.XML).withConversationId
        case Left(errorResult) =>
          common.logger.info("Inventory linking imports request failed")
          errorResult
      }
  }

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) =>
      Right(AnyContentAsXml(xml))
    case _ =>
      Right(AnyContentAsEmpty)
  })
}

@Singleton
class GoodsArrivalController @Inject()(common: Common,
                                       importsMessageType: GoodsArrival,
                                       authAction: GoodsArrivalAuthAction,
                                       payloadValidationAction: GoodsArrivalPayloadValidationAction,
                                       validateAndExtractHeadersAction: ValidateAndExtractHeadersAction)
                                      (implicit ex: ExecutionContext)
  extends ImportController(common: Common,
                           importsMessageType,
                           authAction,
                           payloadValidationAction,
                           validateAndExtractHeadersAction) {

  def post(): Action[AnyContent] = {
    super.process()
  }
}

@Singleton
class ValidateMovementController @Inject()(common: Common,
                                           importsMessageType: ValidateMovement,
                                           authAction: ValidateMovementAuthAction,
                                           payloadValidationAction: ValidateMovementPayloadValidationAction,
                                           validateAndExtractHeadersAction: ValidateMovementValidateAndExtractHeadersAction)
                                          (implicit ex: ExecutionContext)
  extends ImportController(common: Common,
                           importsMessageType,
                           authAction,
                           payloadValidationAction,
                           validateAndExtractHeadersAction) {

  def post(): Action[AnyContent] = {
    super.process()
  }
}
