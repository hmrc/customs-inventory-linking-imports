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

import play.api.http.MimeTypes
import play.api.test.Helpers.{ACCEPT, CONTENT_TYPE}

object TestData {
  object Headers {
    val accept: (String, String) = (ACCEPT, "application/vnd.hmrc.1.0+xml")
    val contentType: (String, String) = (CONTENT_TYPE, MimeTypes.XML)
    val xClientIdName: String = "X-Client-Id"
    val xClientId: (String, String) = (xClientIdName, "c9503c3d-6df7-448d-a01b-e623a3b8806d")
    val xBadgeIdentifierName: String = "X-Badge-Identifier"
    val xBadgeIdentifier: (String, String) = (xBadgeIdentifierName, "BadgeId")

    val validHeaders: Seq[(String, String)] = Seq(accept, contentType, xClientId, xBadgeIdentifier)
  }

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")
}
