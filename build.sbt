name := "WebsocketsAPI"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-stream" % "2.5.4",
  "com.typesafe.akka" %% "akka-actor"  % "2.5.4",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
  "org.scalactic" %% "scalactic" % "3.0.4",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10",
  "org.json4s" % "json4s-native_2.12" % "3.6.0-M1",
  "org.json4s" % "json4s-jackson_2.12" % "3.5.0"
)

