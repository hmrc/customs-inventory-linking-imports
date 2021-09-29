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

  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % scalatestplusVersion % testScope

  val scalatestplusMockito = "org.scalatestplus"   %% "scalatestplus-mockito"    % scalatestplusMockitoVersion   % testScope

  val wireMock = "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % testScope

  val mockito =  "org.mockito" % "mockito-core" % mockitoVersion % testScope

  val flexmark = "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion % testScope

  val Jackson = "com.fasterxml.jackson.module"   %%  "jackson-module-scala"  % JacksonVersion % testScope

  val customsApiCommon = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion withSources()

  val customsApiCommonTests = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion % testScope classifier "tests"

  val silencerPlugin = compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.5" cross CrossVersion.full)
  val silencerLib = "com.github.ghik" % "silencer-lib" % "1.7.5" % Provided cross CrossVersion.full
}
