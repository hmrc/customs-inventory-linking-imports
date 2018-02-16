import AppDependencies._
import org.scalastyle.sbt.ScalastylePlugin._
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import scala.language.postfixOps

name := "customs-inventory-linking-imports"

targetJvm := "jvm-1.8"

lazy val allResolvers = resolvers ++= Seq(
  Resolver.bintrayRepo("hmrc", "releases"),
  Resolver.jcenterRepo
)

val compileDependencies = Seq(microserviceBootStrap, authClient, xmlResolver, customsApiCommon)

val testDependencies = Seq(hmrcTest, scalaTest, pegDown, scalaTestPlusPlay, wireMock, mockito, customsApiCommonTests)

lazy val AcceptanceTest = config("acceptance") extend Test
lazy val CdsIntegrationTest = config("it") extend Test

val testConfig = Seq(AcceptanceTest, CdsIntegrationTest, Test)

lazy val testAll = TaskKey[Unit]("test-all")
lazy val allTest = Seq(testAll := (test in AcceptanceTest)
  .dependsOn((test in CdsIntegrationTest).dependsOn(test in Test)).value)

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    commonSettings,
    unitTestSettings,
    integrationTestSettings,
    acceptanceTestSettings,
    allTest,
    scoverageSettings,
    allResolvers
  )


def forkedJvmPerTestConfig(tests: Seq[TestDefinition], packages: String*): Seq[Group] =
  tests.groupBy(_.name.takeWhile(_ != '.')).filter(packageAndTests => packages contains packageAndTests._1) map {
    case (packg, theTests) =>
      Group(packg, theTests, SubProcess(ForkOptions()))
  } toSeq

def onPackageName(rootPackage: String): (String => Boolean) = {
  testName => testName startsWith rootPackage
}

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      testOptions in Test := Seq(Tests.Filter(onPackageName("unit"))),
      testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      unmanagedSourceDirectories in Test := Seq((baseDirectory in Test).value / "test"),
      addTestReportOption(Test, "test-reports")
    )

lazy val integrationTestSettings =
  inConfig(CdsIntegrationTest)(Defaults.testTasks) ++
    Seq(
      testOptions in CdsIntegrationTest := Seq(Tests.Filters(Seq(onPackageName("integration"), onPackageName("acceptance")))),
      testOptions in CdsIntegrationTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in CdsIntegrationTest := false,
      parallelExecution in CdsIntegrationTest := false,
      addTestReportOption(CdsIntegrationTest, "int-test-reports"),
      testGrouping in CdsIntegrationTest := forkedJvmPerTestConfig((definedTests in Test).value, "integration", "acceptance")
    )

lazy val acceptanceTestSettings =
  inConfig(AcceptanceTest)(Defaults.testTasks) ++
    Seq(
      testOptions in AcceptanceTest := Seq(Tests.Filter(onPackageName("acceptance"))),
      testOptions in AcceptanceTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in AcceptanceTest := false,
      parallelExecution in AcceptanceTest := false,
      addTestReportOption(AcceptanceTest, "acceptance-reports")
    )

lazy val commonSettings: Seq[Setting[_]] = scalaSettings ++
  defaultSettings() ++
  gitStampSettings

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;view.*;models.*;config.*;.*(AuthService|BuildInfo|Routes).*;uk.gov.hmrc.customs.inventorylinking.imports.views.*",
  coverageMinimum := 98,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

unmanagedResourceDirectories in Compile += baseDirectory.value / "public"

libraryDependencies ++= compileDependencies ++ testDependencies

evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
