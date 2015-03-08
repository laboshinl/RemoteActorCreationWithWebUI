name := "MessageRouter"

version := "1.0"

scalaVersion := "2.11.6"

scalacOptions in Test ++= Seq("-Yrangepos")

libraryDependencies ++= {
  val testVersion = "2.2.4"
  val sprayVersion = "1.3.2"
  val akkaVersion = "2.1-M2"
  Seq(
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "com.typesafe.akka" % "akka-actor_2.10.0-M7" % akkaVersion,
    "com.typesafe.akka" % "akka-remote_2.10.0-M7" % akkaVersion,
    "com.typesafe.akka" % "akka-zeromq_2.10.0-M7" % "2.1-M2",
    "org.json4s" %% "json4s-native" % "3.2.11",
    "org.pacesys" % "openstack4j" % "2.0.1",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  )
}