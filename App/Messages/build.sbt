name := "Messages"

version := "1.0"

scalaVersion := "2.10.0"

libraryDependencies ++= {
  val akkaVersion = "2.3.9"
  Seq(
    "com.typesafe.akka" % "akka-actor_2.10" % akkaVersion,
    "com.typesafe.akka" % "akka-zeromq_2.10" % akkaVersion
  )
}