// The Sart sbt autoplugin. Self-contained build, cross-built for both
// plugin ecosystems: Scala 2.12 → sbt 1.x and Scala 3 → sbt 2.x.
// `+publishLocal` publishes both. Users add the plugin via
// `addSbtPlugin("com.outr" % "sbt-sart" % "0.1.0-SNAPSHOT")` after a
// local publish, or via `ProjectRef(file("../sart/sbt-sart"), "sbt-sart")`.
//
// The plugin intentionally does NOT hard-depend on the Scala 3 sart
// modules — it spawns a JVM subprocess to run them, so Scala version
// mismatch is moot.

ThisBuild / organization := "com.outr"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val scala212 = "2.12.20"
lazy val scala3   = "3.8.4"

ThisBuild / crossScalaVersions := Seq(scala212, scala3)

lazy val `sbt-sart` = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-sart",
    sbtPlugin := true,
    // Which sbt API each cross-target compiles against: the 2.12 axis
    // keeps the previous sbt 1.x floor; the Scala 3 axis targets sbt 2.
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.10.7"
        case _      => "2.0.1"
      }
    }
  )
