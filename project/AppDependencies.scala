import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val pekkoSite = "org.apache.pekko"
  val pekkoVersion = "1.0.2" // Do not update as this may cause a Bobby Violation
  val playVersion = "play-30"
  val bootstrapVersion = "8.4.0"
  val hmrcMongoVersion = "1.7.0"
  val compile = Seq(

    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-$playVersion"         % hmrcMongoVersion,
    "uk.gov.hmrc"          %% s"bootstrap-backend-$playVersion"  % bootstrapVersion,
    "org.typelevel"        %% "cats-core"                        % "2.9.0",
    "com.github.fge"       %  "json-schema-validator"            % "2.2.6",
    pekkoSite              %% "pekko-stream"                     % pekkoVersion,
    pekkoSite              %% "pekko-slf4j"                      % pekkoVersion,
    pekkoSite              %% "pekko-actor-typed"                % pekkoVersion,
    pekkoSite              %% "pekko-serialization-jackson"      % pekkoVersion
  )

  val test = Seq(
    "uk.gov.hmrc"              %% s"bootstrap-test-$playVersion"   % bootstrapVersion    % "test",
    "org.scalatest"            %% "scalatest"                      % "3.2.17"            % "test",
    "org.scalacheck"           %% "scalacheck"                     % "1.17.0"            % "test",
    "org.playframework"        %% "play-test"                      % current             % "test",
    "org.mockito"              %  "mockito-core"                   % "5.4.0"             % "test",
    "org.scalatestplus"        %% "mockito-3-4"                    % "3.2.10.0"          % "test, it",
    "org.pegdown"              %  "pegdown"                        % "1.6.0"             % "test, it",
    "org.scalatestplus.play"   %% "scalatestplus-play"             % "7.0.0"             % "test, it",
    "org.scalatestplus"        %% "scalacheck-1-14"                % "3.2.2.0"           % "test, it",
    "com.vladsch.flexmark"     %  "flexmark-all"                   % "0.64.8"            % "test, it",
    "com.github.tomakehurst"   % "wiremock-jre8-standalone"        % "2.35.0"            % "test, it"
  )
}
