import sbt.*

object AppDependencies {

  val pekkoSite = "org.apache.pekko"
  val pekkoVersion = "1.3.0" // Do not update as this may cause a Bobby Violation
  val playVersion = "play-30"
  val bootstrapVersion = "10.7.0"
  val hmrcMongoVersion = "2.12.0"

  val compile: Seq[ModuleID] = Seq(

    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-$playVersion"         % hmrcMongoVersion,
    "uk.gov.hmrc"          %% s"bootstrap-backend-$playVersion"  % bootstrapVersion,
    "org.typelevel"        %% "cats-core"                        % "2.13.0",
    "com.github.fge"       %  "json-schema-validator"            % "2.2.14",
    pekkoSite              %% "pekko-stream"                     % pekkoVersion,
    pekkoSite              %% "pekko-slf4j"                      % pekkoVersion,
    pekkoSite              %% "pekko-actor-typed"                % pekkoVersion,
    (pekkoSite              %% "pekko-serialization-jackson"      % pekkoVersion).exclude("org.lz4","lz4-java")
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"   % bootstrapVersion,
    "org.scalatestplus"      %% "scalacheck-1-17"                % "3.2.18.0",
  ).map(_ % Test)

  val rhinoOverrides: Seq[ModuleID] = Seq("org.mozilla" % "rhino" % "1.9.1")
}
