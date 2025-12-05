import sbt._

object AppDependencies {

  val playVersion = "play-30"
  val bootstrapVersion = "10.4.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                              %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "org.typelevel"                            %% "cats-core"                       % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(

    "org.scalatestplus"                        %% "mockito-4-11"                      % "3.2.18.0"       % Test,
    "org.wiremock"                              % "wiremock-standalone"               % "3.13.1"          % Test,
    "org.mockito"                               % "mockito-core"                      % "5.19.0"          % Test,
    "uk.gov.hmrc"                               %% s"bootstrap-test-$playVersion"     % bootstrapVersion  % Test
  )
}