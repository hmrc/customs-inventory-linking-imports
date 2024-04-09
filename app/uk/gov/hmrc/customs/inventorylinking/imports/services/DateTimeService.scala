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

package uk.gov.hmrc.customs.inventorylinking.imports.services

import java.time._
import java.time.format.DateTimeFormatter

class DateTimeService {
  val UtcZoneId: ZoneId = ZoneId.of("UTC")
  val utcFormattedDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'UTC'")
  val isoFormatNoMillis: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
  def nowUtc(): LocalDateTime = LocalDateTime.ofInstant(Clock.systemUTC().instant(), ZoneOffset.UTC)
  def zonedDateTimeUtc: ZonedDateTime = ZonedDateTime.now(UtcZoneId)
}
