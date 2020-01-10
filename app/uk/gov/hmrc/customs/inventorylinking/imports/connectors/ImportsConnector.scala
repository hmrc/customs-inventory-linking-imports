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

package uk.gov.hmrc.customs.inventorylinking.imports.connectors

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import play.api.http.MimeTypes.XML
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.inventorylinking.imports.logging.ImportsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.HeaderConstants._
import uk.gov.hmrc.customs.inventorylinking.imports.model.actionbuilders.{HasConversationId, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ConversationId, ImportsMessageType}
import uk.gov.hmrc.customs.inventorylinking.imports.services.ImportsConfigService
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{NodeSeq, PrettyPrinter, TopScope}

@Singleton
class ImportsConnector @Inject()(http: HttpClient,
                                 logger: ImportsLogger,
                                 serviceConfigProvider: ServiceConfigProvider,
                                 config: ImportsConfigService)
                                (implicit ex: ExecutionContext) extends UsingCircuitBreaker {

  def send[A](importsMessageType: ImportsMessageType, xml: NodeSeq, date: DateTime, correlationId: UUID)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val config = Option(serviceConfigProvider.getConfig(s"${importsMessageType.name}")).getOrElse(throw new IllegalArgumentException("config not found"))
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    implicit val headerCarrier: HeaderCarrier = hc.copy(extraHeaders = hc.extraHeaders ++ getHeaders(date, correlationId, vpr.conversationId), authorization = Some(Authorization(bearerToken)))
    val startTime = LocalDateTime.now
    withCircuitBreaker(post(xml, config.url)(vpr, headerCarrier))(headerCarrier).map{
      response => {
        logCallDuration(startTime)
        logger.debug(s"Response status ${response.status} and response body ${formatResponseBody(response.body)}")
      }
        response
    }
  }

  private def getHeaders(date: DateTime, correlationId: UUID, conversationId: ConversationId) = {
    Seq(
      (ACCEPT, MimeTypes.XML),
      (CONTENT_TYPE, s"$XML; charset=UTF-8"),
      (DATE, date.toString("EEE, dd MMM yyyy HH:mm:ss z")),
      (X_FORWARDED_HOST, "MDTP"),
      (XConversationId, conversationId.toString),
      (XCorrelationId, correlationId.toString))
  }

  private def post[A](xml: NodeSeq, url: String)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier) = {
    logger.debug(s"Sending request to backend. Url: $url\nPayload: ${xml.toString()}")
    http.POSTString[HttpResponse](url, xml.toString())
      .recoverWith {
        case httpError: HttpException => Future.failed(new RuntimeException(httpError))
      }
      .recoverWith {
        case e: Throwable =>
          logger.error(s"Call to backend failed. url=$url")
          Future.failed(e)
      }
  }

  protected def logCallDuration(startTime: LocalDateTime)(implicit r: HasConversationId): Unit ={
    val callDuration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now)
    logger.info(s"Outbound call duration was ${callDuration} ms")
  }

  private def formatResponseBody(responseBody: String) = {
    if (responseBody.isEmpty) {
      "<empty>"
    } else {
      new PrettyPrinter(120, 2).format(xml.XML.loadString(responseBody), TopScope)
    }
  }

  override protected def circuitBreakerConfig: CircuitBreakerConfig =
    CircuitBreakerConfig(
      serviceName = "customs-inventory-linking-imports",
      numberOfCallsToTriggerStateChange = config.importsCircuitBreakerConfig.numberOfCallsToTriggerStateChange,
      unavailablePeriodDuration = config.importsCircuitBreakerConfig.unavailablePeriodDurationInMillis,
      unstablePeriodDuration = config.importsCircuitBreakerConfig.unstablePeriodDurationInMillis
    )

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case _: BadRequestException | _: NotFoundException | _: Upstream4xxResponse => false
    case _ => true
  }

}
