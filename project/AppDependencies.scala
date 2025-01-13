import sbt._

object AppDependencies {

  val pekkoSite = "org.apache.pekko"
  val pekkoVersion = "1.0.2" // Do not update as this may cause a Bobby Violation
  val playVersion = "play-30"
  val bootstrapVersion = "9.6.0"
  val hmrcMongoVersion = "2.4.0"

  val compile = Seq(

    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-$playVersion"         % hmrcMongoVersion,
    "uk.gov.hmrc"          %% s"bootstrap-backend-$playVersion"  % bootstrapVersion,
    "org.typelevel"        %% "cats-core"                        % "2.12.0",
    "com.github.fge"       %  "json-schema-validator"            % "2.2.14",
    pekkoSite              %% "pekko-stream"                     % pekkoVersion,
    pekkoSite              %% "pekko-slf4j"                      % pekkoVersion,
    pekkoSite              %% "pekko-actor-typed"                % pekkoVersion,
    pekkoSite              %% "pekko-serialization-jackson"      % pekkoVersion
  )

  val test = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"   % bootstrapVersion,
    "org.scalatestplus"      %% "scalacheck-1-17"                % "3.2.18.0",
    "org.mockito"            %% "mockito-scala-scalatest"        % "1.17.37"
  ).map(_ % "test, it")
}
