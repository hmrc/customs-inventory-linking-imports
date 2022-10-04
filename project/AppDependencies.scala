import sbt._

object AppDependencies {

  private val scalatestplusVersion = "4.0.3"
  private val mockitoVersion = "3.11.2"
  private val wireMockVersion = "2.28.1"
  private val customsApiCommonVersion = "1.57.0"
  private val testScope = "test,it"
  private val scalatestplusMockitoVersion = "1.0.0-M2"
  private val flexmarkVersion = "0.19.1"
  private val JacksonVersion = "2.12.3"

  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"          % testScope
  val scalatestplusMockito = "org.scalatestplus"   %% "scalatestplus-mockito"    % "1.0.0-M2"   % testScope
  val wireMock = "com.github.tomakehurst" % "wiremock-jre8" % "2.34.0" % testScope
  val mockito =  "org.mockito" % "mockito-core" % "4.8.0" % testScope
  val flexmark = "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion % testScope
  val Jackson = "com.fasterxml.jackson.module"   %%  "jackson-module-scala"  % "2.14.0-rc1" % testScope
  val customsApiCommon = "uk.gov.hmrc" %% "customs-api-common" % "1.57.0"
  val customsApiCommonTests = "uk.gov.hmrc" %% "customs-api-common" % "1.57.0" % testScope classifier "tests"
  val silencerPlugin = compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.11" cross CrossVersion.full)
  val silencerLib = "com.github.ghik" % "silencer-lib" % "1.7.11" % Provided cross CrossVersion.full
}
