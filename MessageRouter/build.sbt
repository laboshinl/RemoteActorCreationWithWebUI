name := "MessageRouter"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= {
  val testVersion = "2.2.4"
  val akkaVersion = "2.1-M2"
  Seq(
    "com.typesafe.akka" % "akka-actor_2.10.0-M7" % akkaVersion,
    "com.typesafe.akka" % "akka-remote_2.10.0-M7" % akkaVersion,
    "com.typesafe.akka" % "akka-zeromq_2.10.0-M7" % akkaVersion,
    "org.scalatest" % "scalatest_2.11" % testVersion % "test"
  )
}