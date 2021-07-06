import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(

    "uk.gov.hmrc"             %% "simple-reactivemongo"      % "7.31.0-play-26",
    "uk.gov.hmrc"             %% "bootstrap-play-26"       % "4.0.0",
    "org.typelevel"           %% "cats-core"                 % "2.4.2",
    "com.github.fge"          %  "json-schema-validator"     % "2.2.6",
    "com.typesafe.akka"       %% "akka-stream"               % "2.5.18",
    compilerPlugin("com.github.ghik"           % "silencer-plugin" % "1.7.5" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.5" % Provided cross CrossVersion.full
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"                % "3.0.9"                 % "test",
    "org.scalacheck"          %% "scalacheck"               % "1.14.0"                % "test",
    "com.typesafe.play"       %% "play-test"                % current                 % "test",
    "org.mockito"             %  "mockito-core"             % "2.24.0"                % "test",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test, it",
    "uk.gov.hmrc"             %% "service-integration-test" % "1.1.0-play-26"         % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.3"                 % "test, it",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "4.0.0"                 % Test classifier "tests"
  )
}
