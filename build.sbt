import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "vat-registered-companies"

PlayKeys.playDefaultPort := 8731

scalaVersion := "2.13.12"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;views.*;prod.*;.*services.*;.*repositories.*;.*test.*",
    ScoverageKeys.coverageExcludedFiles := "<empty>;.*BuildInfo.*;.*Routes.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    scoverageSettings
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(scalacOptions += "-Wconf:src=routes/.*:s")

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
