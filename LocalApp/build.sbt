name := "LocalApp"

version := "1.0"

scalaVersion := "2.11.5"

scalacOptions in Test ++= Seq("-Yrangepos")

libraryDependencies ++= {
  val testVersion = "2.2.4"
  val sprayVersion = "1.3.2"
  val akkaVersion = "2.3.9"
  Seq(
    "org.specs2" %% "specs2-core" % "2.4.15" % "test",
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-testkit" % sprayVersion % "test",
    "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-remote_2.11" % akkaVersion,
    "org.json4s" %% "json4s-native" % "3.2.11",
    "org.pacesys" % "openstack4j" % "2.0.1"
  )
}