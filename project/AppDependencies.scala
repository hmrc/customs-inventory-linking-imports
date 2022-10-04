import sbt._

object AppDependencies {

  private val testScope = "test,it"

  val scalaTestPlusPlay     = "org.scalatestplus.play"                    %% "scalatestplus-play"       % "5.1.0"          % testScope
  val scalatestplusMockito  = "org.scalatestplus"                         %% "scalatestplus-mockito"    % "1.0.0-M2"   % testScope
  val wireMock              = "com.github.tomakehurst"                     % "wiremock-jre8"            % "2.34.0" % testScope
  val mockito               =  "org.mockito"                               % "mockito-core"             % "4.8.0" % testScope
  val flexmark              = "com.vladsch.flexmark"                       % "flexmark-all"             % "0.19.1" % testScope
  val Jackson               = "com.fasterxml.jackson.module"              %%  "jackson-module-scala"    % "2.14.0-rc1" % testScope
  val customsApiCommon      = "uk.gov.hmrc"                               %% "customs-api-common"       % "1.57.0"
  val customsApiCommonTests = "uk.gov.hmrc"                               %% "customs-api-common"       % "1.57.0" % testScope classifier "tests"
  val silencerPlugin        = compilerPlugin("com.github.ghik" % "silencer-plugin"          % "1.7.11" cross CrossVersion.full)
  val silencerLib           = "com.github.ghik"                            % "silencer-lib"             % "1.7.11" % Provided cross CrossVersion.full
}
