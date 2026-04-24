// The Sart sbt autoplugin. Self-contained build: this project compiles
// to Scala 2.12 (sbt 1.x's plugin runtime), independent of the main
// Scala 3 modules in the parent directory. Users add the plugin via
// `addSbtPlugin("com.outr" % "sbt-sart" % "0.1.0-SNAPSHOT")` after a
// local publish, or via `ProjectRef(file("../sart/sbt-sart"), "sbt-sart")`.
//
// The plugin intentionally does NOT hard-depend on the Scala 3 sart
// modules — it spawns a JVM subprocess to run them, so Scala version
// mismatch is moot.

ThisBuild / organization := "com.outr"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val `sbt-sart` = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-sart",
    sbtPlugin := true,
    pluginCrossBuild / sbtVersion := "1.10.7"
  )
