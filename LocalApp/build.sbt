name := "LocalApp"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= {
  val testVersion = "2.2.4"
  val sprayVersion = "1.3.1"
  val akkaVersion = "2.3.9"
  Seq(
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % sprayVersion,
    "com.typesafe.akka" % "akka-actor_2.10" % akkaVersion,
    "com.typesafe.akka" % "akka-remote_2.10" % akkaVersion,
    "org.json4s" %% "json4s-native" % "3.2.11",
    "org.pacesys" % "openstack4j" % "2.0.1",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  )
}