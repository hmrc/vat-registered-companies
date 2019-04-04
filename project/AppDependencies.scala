import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(

    "uk.gov.hmrc"             %% "play-reactivemongo"       % "6.4.0",
    "uk.gov.hmrc"             %% "simple-reactivemongo"     % "7.12.0-play-25",
    "uk.gov.hmrc"             %% "bootstrap-play-25"        % "4.8.0",
    "org.typelevel"           %% "cats-core"                % "1.1.0",
    "com.github.fge"          %  "json-schema-validator"    % "2.2.6",
    "com.typesafe.akka"       %% "akka-stream"              % "2.5.21"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"                % "3.0.4"                 % "test",
    "org.scalacheck"          %% "scalacheck"               % "1.14.0"                % "test",
    "com.typesafe.play"       %% "play-test"                % current                 % "test",
    "org.mockito"             % "mockito-core"              % "2.24.0"                % "test",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test, it",
    "uk.gov.hmrc"             %% "service-integration-test" % "0.2.0"                 % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "2.0.0"                 % "test, it"
  )
}
