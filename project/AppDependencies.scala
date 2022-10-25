import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(

    "uk.gov.hmrc"             %% "simple-reactivemongo"      % "8.1.0-play-28",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28" % "7.8.0",
    "org.typelevel"           %% "cats-core"                 % "2.8.0",
    "com.github.fge"          %  "json-schema-validator"     % "2.2.6",
    "com.typesafe.akka"       %% "akka-stream"               % "2.6.19",
    compilerPlugin("com.github.ghik"           % "silencer-plugin" % "1.7.11" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.11" % Provided cross CrossVersion.full
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % "7.8.0"             % "test",
    "org.scalatest"           %% "scalatest"                % "3.2.14"             % "test",
    "org.scalacheck"          %% "scalacheck"               % "1.17.0"            % "test",
    "com.typesafe.play"       %% "play-test"                % current             % "test",
    "org.mockito"             %  "mockito-core"             % "4.8.1"            % "test",
    "org.scalatestplus"       %% "mockito-3-4"              % "3.2.10.0"           % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"             % "test, it",
    "uk.gov.hmrc"             %% "service-integration-test" % "1.3.0-play-28"     % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.1.0"             % "test, it",
    "org.scalatestplus"       %% "scalacheck-1-14"          % "3.2.2.0"           % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.62.2"            % "test, it"
  )
}
