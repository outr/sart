ThisBuild / scalaVersion := "3.8.3"
ThisBuild / organization := "com.outr"
ThisBuild / version      := "0.1.0-SNAPSHOT"

// Single-command workflow tasks that chain the Scala → TASTy → Dart →
// Flutter-Linux pipeline. The task keys are defined at the top level so
// they're usable from both the root project and the command line.
val sartEmit  = taskKey[Unit]("Compile the example and emit Dart into out/")
val sartLinux = taskKey[File]("Build a native Linux bundle from the emitted Dart")
val sartRun   = taskKey[Unit]("Build and launch the generated Linux app")

// The facade-support library: annotations + sentinel values that facades
// depend on. Analogous to `scalajs-library` providing `@js.native`, etc.
lazy val `sart-dart` = (project in file("sart-dart"))
  .settings(
    name := "sart-dart"
  )

// Hand-ported Scala stdlib facades that map idiomatically to Dart. Today
// this module only provides `Option`, which maps to Dart's `T?` nullable
// type. Future additions (List, Map, Future, Try, …) follow the same
// template: a Scala facade under `sart.stdlib.*` and a companion Dart
// shim library under `src/main/dart/` that provides the Scala-vocabulary
// extension methods.
lazy val `sart-stdlib` = (project in file("sart-stdlib"))
  .dependsOn(`sart-dart`)
  .settings(
    name := "sart-stdlib"
  )

// Facades for Flutter's material library, written against `sart-dart`.
// Nothing here produces runtime Scala bytecode we care about — the whole
// module exists to give the example code real types to typecheck against
// and to carry the Dart mapping metadata (@DartImport, etc.) in TASTy.
lazy val `flutter-facades` = (project in file("flutter-facades"))
  .dependsOn(`sart-dart`)
  .settings(
    name := "flutter-facades"
  )

// The user program — the pure-Scala counter app. Its `.tasty` files are
// the input to the Sart compiler.
lazy val example = (project in file("example"))
  .dependsOn(`flutter-facades`, `sart-stdlib`)
  .settings(
    name := "sart-example",
    // Keep TASTy around so the compiler can read it.
    Compile / scalacOptions ++= Seq("-Yretain-trees")
  )

// The Scala 3 → Dart compiler itself. Reads TASTy via tasty-inspector and
// writes Dart source to `out/`.
lazy val compiler = (project in file("compiler"))
  .settings(
    name := "sart-compiler",
    libraryDependencies += "org.scala-lang" %% "scala3-tasty-inspector" % scalaVersion.value,
    fork := true
  )

lazy val root = (project in file("."))
  .aggregate(`sart-dart`, `sart-stdlib`, `flutter-facades`, example, compiler)
  .settings(
    name := "sart",

    // Step 1: compile the example, run the Sart compiler on its TASTy, and
    // drop `lib/main.dart` + `pubspec.yaml` into `out/`. This is pure Scala
    // → Dart; no Flutter tooling is invoked here.
    //
    // Def.taskDyn lets us compute the argument line from other tasks'
    // values and then delegate to `runMain`'s InputTask — a plain
    // `task := { ... }` block can't embed `toTask(<dynamic string>).value`.
    sartEmit := Def.taskDyn {
      (example / Compile / compile).value
      val exClasses = (example / Compile / classDirectory).value
      val cp        = (example / Compile / fullClasspath).value
        .map(_.data.getAbsolutePath).mkString(java.io.File.pathSeparator)
      val outDir    = baseDirectory.value / "out"
      IO.createDirectory(outDir)
      streams.value.log.info(s"sart: emitting Dart into $outDir")
      // cp is path-separator-joined so it has no internal spaces —
      // whitespace-splitting in runMain's arg parser leaves it intact.
      val argLine = s" sart.compiler.Main ${exClasses.getAbsolutePath} $cp ${outDir.getAbsolutePath}"
      (compiler / Compile / runMain).toTask(argLine)
    }.value,

    // Step 2: scaffold Flutter's Linux platform files once (idempotent —
    // `flutter create` in an existing directory only adds missing
    // scaffolding; our `lib/main.dart` and `pubspec.yaml` are preserved),
    // then run `flutter build linux`.
    sartLinux := {
      val log = streams.value.log
      sartEmit.value
      val outDir = baseDirectory.value / "out"
      if (!(outDir / "linux").exists()) {
        log.info("sart: scaffolding Flutter Linux platform")
        val rc = sys.process.Process(
          Seq("flutter", "create", "--platforms=linux",
              "--project-name=sart_example", "--org", "com.outr",
              "--suppress-analytics", "."),
          outDir
        ).!
        if (rc != 0) sys.error(s"flutter create exited $rc")
      }
      log.info("sart: flutter build linux")
      val rc = sys.process.Process(
        Seq("flutter", "build", "linux", "--suppress-analytics"),
        outDir
      ).!
      if (rc != 0) sys.error(s"flutter build linux exited $rc")
      val binary = outDir / "build" / "linux" / "x64" / "release" / "bundle" / "sart_example"
      log.info(s"sart: built $binary")
      binary
    },

    // Step 3: convenience — build then exec the binary.
    sartRun := {
      val log    = streams.value.log
      val binary = sartLinux.value
      log.info(s"sart: launching $binary")
      val rc = sys.process.Process(binary.getAbsolutePath).!
      if (rc != 0) sys.error(s"app exited $rc")
    }
  )
