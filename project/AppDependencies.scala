import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(

    "uk.gov.hmrc"             %% "simple-reactivemongo"     % "7.30.0-play-26",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "2.0.0",
    "org.typelevel"           %% "cats-core"                % "1.1.0",
    "com.github.fge"          %  "json-schema-validator"    % "2.2.6",
    "com.typesafe.akka"       %% "akka-stream"              % "2.5.18"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"                % "3.0.4"                 % "test",
    "org.scalacheck"          %% "scalacheck"               % "1.14.0"                % "test",
    "com.typesafe.play"       %% "play-test"                % current                 % "test",
    "org.mockito"             %  "mockito-core"             % "2.24.0"                % "test",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test, it",
    "uk.gov.hmrc"             %% "service-integration-test" % "0.13.0-play-26"        % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.2"                 % "test, it",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "2.0.0" % Test classifier "tests"
  )
}
