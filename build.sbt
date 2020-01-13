import AppDependencies._
import org.scalastyle.sbt.ScalastylePlugin._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt.{Resolver, _}
import uk.gov.hmrc.gitstamp.GitStampPlugin._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.PublishingSettings._
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import scala.language.postfixOps

name := "customs-inventory-linking-imports"

targetJvm := "jvm-1.8"

lazy val allResolvers = resolvers ++= Seq(
  Resolver.bintrayRepo("hmrc", "releases"),
  Resolver.jcenterRepo
)

val compileDependencies = Seq(customsApiCommon, circuitBreaker)

val testDependencies = Seq(hmrcTest, scalaTest, scalaTestPlusPlay, wireMock, mockito, customsApiCommonTests)

lazy val ComponentTest = config("component") extend Test
lazy val CdsIntegrationTest = config("it") extend Test

val testConfig = Seq(ComponentTest, CdsIntegrationTest, Test)

def forkedJvmPerTestConfig(tests: Seq[TestDefinition], packages: String*): Seq[Group] =
  tests.groupBy(_.name.takeWhile(_ != '.')).filter(packageAndTests => packages contains packageAndTests._1) map {
    case (packg, theTests) =>
      Group(packg, theTests, SubProcess(ForkOptions()))
  } toSeq

lazy val testAll = TaskKey[Unit]("test-all")
lazy val allTest = Seq(testAll := (test in ComponentTest)
  .dependsOn((test in CdsIntegrationTest).dependsOn(test in Test)))

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .enablePlugins(SbtArtifactory)
  .enablePlugins(SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    commonSettings,
    unitTestSettings,
    integrationTestSettings,
    componentTestSettings,
    playPublishingSettings,
    allTest,
    scoverageSettings,
    allResolvers
  ).settings(majorVersion := 0)

def onPackageName(rootPackage: String): String => Boolean = {
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
      testOptions in CdsIntegrationTest := Seq(Tests.Filters(Seq(onPackageName("integration"), onPackageName("component")))),
      testOptions in CdsIntegrationTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in CdsIntegrationTest := false,
      parallelExecution in CdsIntegrationTest := false,
      addTestReportOption(CdsIntegrationTest, "int-test-reports"),
      testGrouping in CdsIntegrationTest := forkedJvmPerTestConfig((definedTests in Test).value, "integration", "component")
    )

lazy val componentTestSettings =
  inConfig(ComponentTest)(Defaults.testTasks) ++
    Seq(
      testOptions in ComponentTest := Seq(Tests.Filter(onPackageName("component"))),
      testOptions in ComponentTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in ComponentTest := false,
      parallelExecution in ComponentTest := false,
      addTestReportOption(ComponentTest, "component-reports")
    )


lazy val commonSettings: Seq[Setting[_]] =
  scalaSettings ++
  publishingSettings ++
  defaultSettings() ++
  gitStampSettings

lazy val playPublishingSettings: Seq[sbt.Setting[_]] = sbtrelease.ReleasePlugin.releaseSettings ++
  Seq(credentials += SbtCredentials) ++
  publishAllArtefacts

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := "<empty>;models/.data/..*;view.*;models.*;config.*;.*(Reverse|AuthService|BuildInfo|Routes).*;uk.gov.hmrc.customs.inventorylinking.imports.views.*",
  coverageMinimum := 97,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

unmanagedResourceDirectories in Compile += baseDirectory.value / "public"

libraryDependencies ++= compileDependencies ++ testDependencies

// Task to create a ZIP file containing all inventory linking imports XSDs for each version, under the version directory
lazy val zipXsds = taskKey[Unit]("Zips up all inventory linking imports XSD's")
zipXsds := {
  (baseDirectory.value / "public" / "api" / "conf")
    .listFiles()
    .filter(_.isDirectory)
    .foreach { dir =>
      val wcoXsdDir = dir / "schemas" / "imports"
      val zipFile = dir / "inventory-linking-imports-schemas.zip"
      IO.zip(Path.allSubpaths(wcoXsdDir), zipFile)
    }
}

// default package task depends on packageBin which we override here to also invoke the custom ZIP task
packageBin in Compile := {
  zipXsds.value
  (packageBin in Compile).value
}

evictionWarningOptions in update := EvictionWarningOptions.default.withWarnTransitiveEvictions(false)
