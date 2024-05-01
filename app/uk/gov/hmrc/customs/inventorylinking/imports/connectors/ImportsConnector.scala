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

package uk.gov.hmrc.customs.inventorylinking.imports.connectors

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.apache.pekko.actor.ActorSystem

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames._
import play.api.http.MimeTypes.XML
import play.api.http.{MimeTypes, Status}
import uk.gov.hmrc.customs.inventorylinking.imports.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{HasConversationId, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ConversationId, ImportsMessageType}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{DateTimeService, ImportsConfigService}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.NodeSeq

@Singleton
class ImportsConnector @Inject()(http: HttpClient,
                                 logger: ImportsLogger,
                                 serviceConfigProvider: ServiceConfigProvider,
                                 config: ImportsConfigService,
                                 override val cdsLogger: CdsLogger,
                                 override val actorSystem: ActorSystem)
                                (implicit override val ec: ExecutionContext) extends CircuitBreakerConnector with HttpErrorFunctions with Status {

  override val configKey = "mdg-imports"

  override lazy val numberOfCallsToTriggerStateChange = config.importsCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis = config.importsCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis = config.importsCircuitBreakerConfig.unavailablePeriodDurationInMillis

  def send[A](importsMessageType: ImportsMessageType, xml: NodeSeq, date: LocalDateTime, correlationId: UUID)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val config = Option(serviceConfigProvider.getConfig(s"${vpr.requestedApiVersion.configPrefix}${importsMessageType.name}")).getOrElse(throw new IllegalArgumentException("config not found"))
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))

    implicit val headerCarrier: HeaderCarrier = hc.copy(authorization = None)
    val importsHeaders = hc.extraHeaders ++ getHeaders(date, correlationId, vpr.conversationId) ++ Seq(HeaderNames.authorisation -> bearerToken) ++ hc.headers(List("Accept", "Gov-Test-Scenario"))
    val startTime = LocalDateTime.now
    withCircuitBreaker(post(xml, config.url, importsHeaders)(vpr, headerCarrier))
      .map { response =>
        logCallDuration(startTime)
        logger.debug(s"Response status ${response.status} and response body ${formatResponseBody(response.body)}")
        response
      }
  }

  private def getHeaders(date: LocalDateTime, correlationId: UUID, conversationId: ConversationId) = {
    val utcDateFormat: DateTimeFormatter = new DateTimeService().utcFormattedDate
    Seq(
      (ACCEPT, MimeTypes.XML),
      (CONTENT_TYPE, s"$XML; charset=UTF-8"),
      (DATE, date.atOffset(ZoneOffset.UTC).format(utcDateFormat)),
      (X_FORWARDED_HOST, "MDTP"),
      (XConversationId, conversationId.toString),
      (XCorrelationId, correlationId.toString))
  }

  private def post[A](xml: NodeSeq, url: String, importsHeaders: Seq[(String, String)])(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier) = {
    logger.debug(s"Sending request to backend. Url: $url\nPayload: ${xml.toString()}")
    http.POSTString[HttpResponse](url, xml.toString(), headers = importsHeaders).map { response =>
      response.status match {
        case status if is2xx(status) =>
          response
        case status => //1xx, 3xx, 4xx, 5xx
          throw new Non2xxResponseException(s"Call to Inventory Linking Imports backend failed. Status=[$status] url=[$url] response body=[${formatResponseBody(response.body)}]", status)
      }
    }.recoverWith {
      case httpError: HttpException =>
        Future.failed(httpError)
      case NonFatal(e) =>
        logger.error(s"Call to backend failed. url=[$url]", e)
        Future.failed(e)
    }
  }

  protected def logCallDuration(startTime: LocalDateTime)(implicit r: HasConversationId): Unit = {
    val callDuration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now)
    logger.info(s"Outbound call duration was ${callDuration} ms")
  }

  private def formatResponseBody(responseBody: String) = {
    if (responseBody.isEmpty) {
      "<empty>"
    } else {
      responseBody
    }
  }

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case e: Non2xxResponseException => e.responseCode match {
      case BAD_REQUEST => false
      case NOT_FOUND => false
      case _ => true
    }
    case _ => true
  }
}
