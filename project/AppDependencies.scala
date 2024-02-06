import sbt._

object AppDependencies {

  private val testScope = "test,it"

  val bootstrapBackendPlay28 = "uk.gov.hmrc"                              %% "bootstrap-backend-play-28" % "8.0.0"
  val cats                  = "org.typelevel"                             %% "cats-core"               % "2.9.0"
  val scalaTestPlusPlay     = "org.scalatestplus.play"                    %% "scalatestplus-play"      % "5.1.0"    % testScope
  val scalatestplusMockito  = "org.scalatestplus"                         %% "scalatestplus-mockito"   % "1.0.0-M2" % testScope
  val wireMock              = "com.github.tomakehurst"                    % "wiremock-standalone"      % "2.27.2"   % testScope
  val mockito               =  "org.mockito"                              % "mockito-core"             % "5.3.1"    % testScope
  val flexmark               = "com.vladsch.flexmark"                       % "flexmark-all"              % "0.35.10"  % testScope
  val Jackson               = "com.fasterxml.jackson.module"              %% "jackson-module-scala"    % "2.15.1"   % testScope
  val bootstrapTestPlay      = "uk.gov.hmrc"                              %% "bootstrap-test-play-28"    % "8.2.0"    % testScope
}
