name := "er-data-producer"

version := "0.1"

scalaVersion := "2.12.8"

val circeVersion = "0.10.0"

val catsEffectVersion = "1.1.0"

libraryDependencies ++= Seq (
  "org.typelevel" %% "cats-effect" % catsEffectVersion,

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,

  "ch.qos.logback" % "logback-classic" % "1.2.3"
)