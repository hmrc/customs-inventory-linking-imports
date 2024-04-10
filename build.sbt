import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline
import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt.{IO, Path, Setting, SimpleFileFilter, taskKey, _}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, targetJvm}
import uk.gov.hmrc.gitstamp.GitStampPlugin._
import uk.gov.hmrc.DefaultBuildSettings

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.language.postfixOps


val appName = "customs-inventory-linking-imports"

// move shared settings from `microservice` here
ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

def forkedJvmPerTestConfig(tests: Seq[TestDefinition], packages: String*): Seq[Group] =
  tests.groupBy(_.name.takeWhile(_ != '.')).filter(packageAndTests => packages contains packageAndTests._1) map {
    case (packg, theTests) =>
      Group(packg, theTests, SubProcess(ForkOptions()))
  } toSeq

lazy val testAll = TaskKey[Unit]("test-all")

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    targetJvm := "jvm-11",
    commonSettings,
    unitTestSettings,
    scoverageSettings
  )
  .settings(playDefaultPort := 9824)
  .settings(scalacOptions ++= List(
    "-Wconf:cat=unused-imports&src=target/scala-2\\.13/routes/.*:s"
  ))

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      Test / testOptions := Seq(Tests.Filter(unitTestFilter)),
      Test / unmanagedSourceDirectories := Seq((Test / baseDirectory).value / "test"),
      addTestReportOption(Test, "test-reports")
    )

lazy val commonSettings: Seq[Setting[_]] = gitStampSettings

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := "<empty>;models/.data/..*;view.*;models.*;config.*;.*(Reverse|AuthService|BuildInfo|Routes).*;uk.gov.hmrc.customs.inventorylinking.imports.views.*",
  coverageMinimumStmtTotal := 98,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  Test / parallelExecution := false
)

def integrationComponentTestFilter(name: String): Boolean = (name startsWith "integration") || (name startsWith "component")
def unitTestFilter(name: String): Boolean = name startsWith "unit"

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

Compile / unmanagedResourceDirectories += baseDirectory.value / "public"
(Runtime / managedClasspath) += (Assets / packageBin).value

libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(
    DefaultBuildSettings.itSettings(),
    parallelExecution := false)
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(scoverageSettings: _*)

addCommandAlias("runAllChecks", "clean;compile;scalastyle;coverage;test;it/test;coverageReport;dependencyUpdates")

scalacOptions += "-Wconf:cat=unused-imports&src=routes/.*:s"

// Task to create a ZIP file containing all xsds for each version, under the version directory
val zipXsds = taskKey[Pipeline.Stage]("Zips up all inventory linking imports XSDs")

zipXsds := { mappings: Seq[PathMapping] =>
  val targetDir = WebKeys.webTarget.value / "zip"
  val zipFiles: Iterable[java.io.File] =
    ((Assets / resourceDirectory).value / "api" / "conf")
      .listFiles
      .filter(_.isDirectory)
      .map { dir =>
        val xsdPaths = Path.allSubpaths(dir / "schemas")
        val exampleMessagesFilter = new SimpleFileFilter(_.getPath.contains("/annotated_XML_samples/"))
        val exampleMessagesPaths = Path.selectSubpaths(dir / "examples", exampleMessagesFilter)
        val zipFile = targetDir / "api" / "conf" / dir.getName / "inventory-linking-imports-schemas.zip"
        IO.zip(xsdPaths ++ exampleMessagesPaths, zipFile, None)
        val sizeInMb = (BigDecimal(zipFile.length) / BigDecimal(1024 * 1024)).setScale(1, BigDecimal.RoundingMode.UP)
        println(s"Created zip $zipFile")
        val today = Calendar.getInstance().getTime()
        val dateFormat = new SimpleDateFormat("dd/MM/YYYY")
        val lastUpdated = dateFormat.format(today)
        println(s"Update the file size in ${dir.getParent}/${dir.getName}/docs/schema.md to be [ZIP, ${sizeInMb}MB last updated $lastUpdated]")
        println(s"Check the yaml renders correctly file://${dir.getParent}/${dir.getName}/application.yaml")
        println("")
        zipFile
      }
  zipFiles.pair(Path.relativeTo(targetDir)) ++ mappings
}

pipelineStages := Seq(zipXsds)
