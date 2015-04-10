import sbt._
import Keys._

/**
 * based on https://github.com/harrah/xsbt/wiki/Getting-Started-Multi-Project
 */
object HelloBuild extends Build {

  // aggregate: running a task on the aggregate project will also run it on the aggregated projects.
  // dependsOn: a project depends on code in another project.
  // without dependsOn, you'll get a compiler error: "object bar is not a member of package
  // com.alvinalexander".
  lazy val rootApp = Project(id = "root",
                          base = file(".")) aggregate(remoteApp, messageRouter, localApp, core) dependsOn(remoteApp, messageRouter, localApp, core)

  // sub-project in the Foo subdirectory
  lazy val remoteApp = Project(id = "ra",
                         base = file("RemoteApp")) aggregate(core) dependsOn(core)

  // sub-project in the Bar subdirectory
  lazy val messageRouter = Project(id = "mr",
                         base = file("MessageRouter")) aggregate(core) dependsOn(core)

  lazy val localApp = Project(id = "la",
                         base = file("LocalApp")) aggregate(core) dependsOn(core)

  lazy val core = Project(id = "core",
                         base = file("CoreLibrary"))
}