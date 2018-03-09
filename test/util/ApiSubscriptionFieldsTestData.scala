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

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ApiSubscriptionFieldsResponse, ApiSubscriptionKey, FieldsId}
import util.ExternalServicesConfig.{Host, Port}
import util.externalservices.InventoryLinkingImportsExternalServicesConfig._

object ApiSubscriptionFieldsTestData {
  val FieldsIdAsString = "327d9145-4965-4d28-a2c5-39dedee50334"
  val TestFieldsId = FieldsId(FieldsIdAsString)
  val TestXClientId = "SOME_X_CLIENT_ID"
  val ApiContext = "some/api/context"
  val ApiContextEncoded = "some%2Fapi%2Fcontext"
  val TestApiVersion = "1.0"
  val TestApiSubscriptionKey = ApiSubscriptionKey(TestXClientId, ApiContext, TestApiVersion)
  val TestApiSubscriptionKeyWithEncodedContext: ApiSubscriptionKey = TestApiSubscriptionKey.copy(context = ApiContextEncoded)
  val TestApiSubscriptionFieldsResponse = ApiSubscriptionFieldsResponse(UUID.fromString(FieldsIdAsString))
  val ResponseJsonString: String =
    s"""{
       |  "clientId": "afsdknbw34ty4hebdv",
       |  "apiContext": "ciao-api",
       |  "apiVersion": "1.0",
       |  "fieldsId":"$FieldsIdAsString",
       |  "fields":{
       |    "callback-id":"http://localhost",
       |    "token":"abc123"
       |  }
       |}""".stripMargin

  lazy val ValidConfig: Config = ConfigFactory.parseString(
    s"""
       |Test {
       |  microservice {
       |    services {
       |      api-subscription-fields {
       |        host = $Host
       |        port = $Port
       |        context = $ApiSubscriptionFieldsContext
       |      }
       |    }
       |  }
       |}
    """.stripMargin)

  lazy val InvalidConfigMissingHost: Config = ConfigFactory.parseString(
    s"""
       |Test {
       |  microservice {
       |    services {
       |      api-subscription-fields {
       |        port = $Port
       |        context = $ApiSubscriptionFieldsContext
       |      }
       |    }
       |  }
       |}
    """.stripMargin)

  lazy val InvalidConfigMissingContext: Config = ConfigFactory.parseString(
    s"""
       |Test {
       |  microservice {
       |    services {
       |      api-subscription-fields {
       |        host = $Host
       |        port = $Port
       |      }
       |    }
       |  }
       |}
    """.stripMargin)

}
