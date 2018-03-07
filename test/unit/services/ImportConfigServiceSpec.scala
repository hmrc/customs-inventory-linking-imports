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

package unit.services

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import uk.gov.hmrc.customs.api.common.config.ConfigValidationNelAdaptor
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.inventorylinking.imports.model.{ApiDefinitionConfig, ImportsConfig}
import uk.gov.hmrc.customs.inventorylinking.imports.services.ImportsConfigService
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec
import util.MockitoPassByNameHelper.PassByNameVerifier

class ImportConfigServiceSpec extends UnitSpec with MockitoSugar {
  private val validAppConfig: Config = ConfigFactory.parseString(
    """
      |customs.definition.api-scope = "write:customs-inventory-linking-imports"
      |api.access.version-1.0.whitelistedApplicationIds.0 = someId-1
      |api.access.version-1.0.whitelistedApplicationIds.1 = someId-2
    """.stripMargin)

  private val emptyAppConfig: Config = ConfigFactory.parseString("")

  private val validServicesConfiguration = Configuration(validAppConfig)
  private val emptyServicesConfiguration = Configuration(emptyAppConfig)
  private val mockCdsLogger = mock[CdsLogger]

  private def customsConfigService(conf: Configuration): ImportsConfig =
    new ImportsConfigService(new ConfigValidationNelAdaptor(testServicesConfig(conf), conf), mockCdsLogger)

  "ImportsConfigService" should {
    "return config as object model when configuration is valid" in {
      val configService = customsConfigService(validServicesConfiguration)

      configService.apiDefinitionConfig shouldBe ApiDefinitionConfig("write:customs-inventory-linking-imports", Seq("someId-1", "someId-2"))
    }

    "throw an exception when configuration is invalid, that contains AGGREGATED error messages" in {
      val expectedErrorMessage = "\nCould not find config key 'customs.definition.api-scope'"

      val caught = intercept[IllegalStateException](customsConfigService(emptyServicesConfiguration))

      caught.getMessage shouldBe expectedErrorMessage
      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam[String](expectedErrorMessage)
        .verify()
    }
  }

  private def testServicesConfig(configuration: Configuration) = new ServicesConfig {
    override val runModeConfiguration = configuration
    override val mode = play.api.Mode.Test

    override def environment: Environment = mock[Environment]
  }

}