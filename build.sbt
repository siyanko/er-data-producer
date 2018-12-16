name := "er-data-producer"

version := "0.1"

scalaVersion := "2.12.8"

val circeVersion = "0.10.0"

val catsEffectVersion = "1.1.0"

val http4sVersion = "0.20.0-M4"

scalacOptions += "-Ypartial-unification"


libraryDependencies ++= Seq (
  "org.typelevel" %% "cats-effect" % catsEffectVersion,

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,

  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,

  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)