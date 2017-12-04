import sbt._

object AppDependencies {
  private val xmlResolverVersion = "1.2"
  private val microserviceBootstrapVersion = "6.12.0"
  private val authClientVersion = "2.3.0"
  private val hmrcTestVersion = "3.0.0"
  private val scalaTestVersion = "3.0.4"
  private val scalatestplusVersion = "2.0.1"
  private val mockitoVersion = "2.6.2"
  private val pegdownVersion = "1.6.0"
  private val wireMockVersion = "2.10.1"
  private val testScope = "test,it"

  val xmlResolver = "xml-resolver" % "xml-resolver" % xmlResolverVersion

  val microserviceBootStrap = "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion

  val authClient =  "uk.gov.hmrc" %% "auth-client" % authClientVersion

  val hmrcTest = "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % testScope

  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % testScope

  val pegDown = "org.pegdown" % "pegdown" % pegdownVersion % testScope

  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % scalatestplusVersion % testScope

  val wireMock = "com.github.tomakehurst" % "wiremock" % wireMockVersion % testScope exclude("org.apache.httpcomponents","httpclient") exclude("org.apache.httpcomponents","httpcore")

  val mockito =  "org.mockito" % "mockito-core" % mockitoVersion % testScope
}
