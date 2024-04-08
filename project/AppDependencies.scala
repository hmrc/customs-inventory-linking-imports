import sbt._

object AppDependencies {

  val playVersion = "play-30"
  val bootstrapVersion = "8.5.0"

  val compile = Seq(
    "uk.gov.hmrc"                              %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "org.typelevel"                            %% "cats-core"                       % "2.10.0"
  )

  val test = Seq(
  "org.scalatestplus.play"                    %% "scalatestplus-play"               % "7.0.1"           % Test,
  "org.scalatestplus"                         %% "mockito-4-2"                      % "3.2.11.0"        % Test,
  "org.wiremock"                              % "wiremock-standalone"               % "3.4.2"           % Test,
  "org.mockito"                               % "mockito-core"                      % "5.11.0"          % Test,
  "com.vladsch.flexmark"                      % "flexmark-all"                      % "0.64.8"          % Test,
  "com.fasterxml.jackson.module"              %% "jackson-module-scala"             % "2.17.0"          % Test,
  "uk.gov.hmrc"                               %% s"bootstrap-test-$playVersion"     % bootstrapVersion  % Test
  )
}
