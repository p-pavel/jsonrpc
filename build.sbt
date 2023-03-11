name         := "scratch-link"
scalaVersion := "3.2.2"
scalacOptions ++= Seq(
  "-Xcheck-macros",
  "-Ykind-projector:underscores",
  "-explain",
  "-deprecation",
  "-rewrite",
  "-indent",
  "-source",
  "future-migration"
)
libraryDependencies ++=
  Seq("dsl")
    .map(a => "org.http4s" %% ("http4s-" + a) % "0.23.18")

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-jdk-http-client" % "0.9.0"
)

libraryDependencies += "org.typelevel" %% "cats-effect-kernel" % "3.4.8"

libraryDependencies += "com.outr" %% "scribe-cats" % "3.11.1"

val circeVersion = "0.14.5"

libraryDependencies ++= Seq(
  "circe-core",
  "circe-generic",
  "circe-parser"
).map("io.circe" %% _ % circeVersion)

libraryDependencies += "com.outr" %% "scribe-slf4j" % "3.11.1"
