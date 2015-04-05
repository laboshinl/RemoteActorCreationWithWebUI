name := "Messages"

version := "1.0"

scalaVersion := "2.10.0"

libraryDependencies ++= {
  val testVersion = "2.2.4"
  val akkaVersion = "2.3.9"
  Seq(
    "com.typesafe.akka" % "akka-actor_2.10" % akkaVersion,
    "com.typesafe.akka" % "akka-remote_2.10" % akkaVersion,
    "com.typesafe.akka" % "akka-zeromq_2.10" % akkaVersion,
    "org.json4s" %% "json4s-native" % "3.2.11",
    "org.scalatest" % "scalatest_2.10" % testVersion % "test"
  )
}