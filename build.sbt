val appName = "vat-registered-companies"

PlayKeys.playDefaultPort := 8731

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.7.4"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;views.*;prod.*;.*services.*;.*repositories.*;.*test.*",
    ScoverageKeys.coverageExcludedFiles    := "<empty>;.*BuildInfo.*;.*Routes.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 81,
    ScoverageKeys.coverageFailOnMinimum    := true,
    ScoverageKeys.coverageHighlighting     := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    dependencyOverrides ++=AppDependencies.rhinoOverrides
  )
  .settings(scoverageSettings *)
  .settings(
    scalacOptions += "-Wconf:msg=routes/.*:s",
    scalacOptions += "-Wconf:msg=Flag.*repeatedly:s",
    scalacOptions += "-Wconf:msg=unused explicit parameter*:s",
    scalacOptions += "-Wconf:msg=unused private member*:s",
    scalacOptions += "-Wconf:msg=unused import*:s",
    scalacOptions += "-Wconf:msg=unused local definition*:s",
    scalacOptions += "-Wconf:msg=unused pattern variable:s"
  )

addCommandAlias("testAll", "; test ; it/test")

libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always



lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "compile->compile,test;test->compile,test")
  .settings(
    Test / scalaSource := baseDirectory.value / "test" / "scala",
    libraryDependencies ++= AppDependencies.test
  )

