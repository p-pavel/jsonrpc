name         := "scratch-link"
scalaVersion := "3.3.1"
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
libraryDependencies += "eu.timepit" %% "refined-cats" % "0.10.2"
libraryDependencies += "org.typelevel" %% "cats-laws" % "2.9.0" % Test 

libraryDependencies += "org.typelevel" %% "cats-effect" % "3.4.8" 
libraryDependencies += "org.typelevel" %% "cats-effect-laws" % "3.4.8" % Test

libraryDependencies += "com.outr" %% "scribe-cats" % "3.11.1"

val circeVersion = "0.14.5"

libraryDependencies ++= Seq(
  "circe-core",
  "circe-generic",
  "circe-parser"
).map("io.circe" %% _ % circeVersion)

libraryDependencies += "io.circe" %% "circe-testing" % circeVersion % Test

libraryDependencies += "com.outr" %% "scribe-slf4j" % "3.11.1"
// libraryDependencies += "com.softwaremill.magnolia1_3" %% "magnolia" % "1.3.0"
// libraryDependencies += "org.typelevel" %% "shapeless3-deriving" % "3.3.0"

libraryDependencies += "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
libraryDependencies += "org.typelevel" %% "discipline-munit" % "2.0.0-M3" % Test

