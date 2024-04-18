import sbt._

object AppDependencies {

  val playVersion = "play-30"
  val bootstrapVersion = "8.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                              %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "org.typelevel"                            %% "cats-core"                       % "2.10.0"
  )

  val test: Seq[ModuleID] = Seq(
  "org.mockito"                               %% "mockito-scala-scalatest"          % "1.17.31"         % Test,
  "org.wiremock"                              % "wiremock-standalone"               % "3.5.2"           % Test,
  "org.mockito"                               % "mockito-core"                      % "5.11.0"          % Test,
  "uk.gov.hmrc"                               %% s"bootstrap-test-$playVersion"     % bootstrapVersion  % Test
  )
}
