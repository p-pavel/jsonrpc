name := "scratch-link"
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
libraryDependencies += "com.outr" %% "scribe" % "3.11.1"