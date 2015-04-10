name := "RemoteApp"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= {
  val testVersion = "1.9.2"
  val akkaVersion = "2.3.9"
  val jsonVersion = "3.2.11"
  Seq(
    "com.typesafe.akka" % "akka-actor_2.10" % akkaVersion,
    "com.typesafe.akka" % "akka-remote_2.10" % akkaVersion,
    "com.typesafe.akka" % "akka-zeromq_2.10" % akkaVersion,
    "org.scalatest" % "scalatest_2.10" % testVersion,
    "org.json4s" %% "json4s-native" % jsonVersion
  )
}