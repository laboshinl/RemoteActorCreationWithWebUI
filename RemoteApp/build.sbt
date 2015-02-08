name := "RemoteApp"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= {
  val akkaVersion = "2.3.9"
  Seq(
    "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-remote_2.11" % akkaVersion,
    "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
  )
}