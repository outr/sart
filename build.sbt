ThisBuild / scalaVersion := "3.8.3"
ThisBuild / organization := "com.outr"
ThisBuild / version      := "0.1.0-SNAPSHOT"

// Publishing metadata for Maven Central / Sonatype. Set here on
// `ThisBuild` so every sub-module inherits. Credentials / signing are
// intentionally NOT in source — configure via ~/.sbt/1.0/sonatype.sbt
// (see https://github.com/xerial/sbt-sonatype). Running `sbt
// sartPublishLocalAll` stays credential-free; `sbt publishSigned` is
// the gesture that pushes to Central.
ThisBuild / organizationName := "OUTR"
ThisBuild / organizationHomepage := Some(url("https://outr.com"))
ThisBuild / homepage := Some(url("https://github.com/outr/sart"))
ThisBuild / licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / scmInfo := Some(ScmInfo(
  url("https://github.com/outr/sart"),
  "scm:git:git@github.com:outr/sart.git"
))
ThisBuild / developers := List(
  Developer(
    id    = "darkfrog26",
    name  = "Matt Hicks",
    email = "matt@outr.com",
    url   = url("https://github.com/darkfrog26")
  )
)
ThisBuild / description :=
  "Scala 3 → Dart / Flutter compiler. Write Flutter apps in Scala; " +
    "ship on Linux, web, Android, iOS, macOS, and Windows."

// Publish to Sonatype by default — the URL sbt needs depends on whether
// we're cutting a snapshot (OSSRH snapshots repo) or a release (staging).
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                  Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

// Single-command workflow tasks that chain the Scala → TASTy → Dart →
// Flutter-Linux pipeline. The task keys are defined at the top level so
// they're usable from both the root project and the command line.
val sartEmit          = taskKey[Unit]("Compile the example and emit Dart into out/")
val sartLinux         = taskKey[File]("Build a native Linux bundle from the emitted Dart")
val sartRun           = taskKey[Unit]("Build and launch the generated Linux app")
val sartGoldenVerify  = taskKey[Unit]("Emit Dart and diff it against the checked-in golden files")
val sartGoldenAccept  = taskKey[Unit]("Emit Dart and overwrite the golden files with the new output")
val sartAnalyze       = taskKey[Unit]("Run flutter analyze on out/ and map errors back to Scala sources")
val sartAnalyzeOnly   = taskKey[Unit]("Run flutter analyze on the existing out/ without re-emitting (for testing the mapper)")
val sartWatch         = taskKey[Unit]("Hint: run with `sbt ~sartEmit` — documented here so `sbt sartWatch` prints usage")
val sartPublishLocalAll = taskKey[Unit]("Publish all Sart core modules + the sbt-sart plugin to the local Ivy repo")
val sartWeb           = taskKey[File]("Build a Flutter web bundle from the emitted Dart")
val sartAndroid       = taskKey[File]("Build a Flutter Android debug APK from the emitted Dart")
val sartMacOS         = taskKey[File]("Build a Flutter macOS bundle (requires a macOS host)")
val sartWindows       = taskKey[File]("Build a Flutter Windows bundle (requires a Windows host)")
val sartIOS           = taskKey[File]("Build a Flutter iOS bundle — no-codesign (requires a macOS host + Xcode)")

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
// Also depends on `sart-stdlib` so Flutter widgets whose Dart signatures
// reference stdlib types (e.g. `StreamBuilder` taking a `Stream<T>`) can
// carry those types directly in the facade.
lazy val `flutter-facades` = (project in file("flutter-facades"))
  .dependsOn(`sart-dart`, `sart-stdlib`)
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

// Facade generator: reads Dart source through a small Dart-side helper
// (invoked as a subprocess), parses the resulting JSON, and emits Scala
// `@native` facades. Enables bulk authoring of facades for pub.dev
// libraries rather than hand-writing every class.
lazy val `sart-facadegen` = (project in file("sart-facadegen"))
  .settings(
    name := "sart-facadegen",
    fork := true
  )

lazy val root = (project in file("."))
  .aggregate(`sart-dart`, `sart-stdlib`, `flutter-facades`, example, compiler, `sart-facadegen`)
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
      val log       = streams.value.log
      IO.createDirectory(outDir)
      log.info(s"sart: emitting Dart into $outDir")
      // cp is path-separator-joined so it has no internal spaces —
      // whitespace-splitting in runMain's arg parser leaves it intact.
      // The 4th arg is the source root that the emitter uses to
      // relativise `/// Source:` attribution comments.
      val sourceRoot = baseDirectory.value.getAbsolutePath
      val argLine = s" sart.compiler.Main ${exClasses.getAbsolutePath} $cp ${outDir.getAbsolutePath} $sourceRoot"
      val emit = (compiler / Compile / runMain).toTask(argLine)
      Def.task {
        emit.value
        // Run `dart format` on the emitted lib/ so the output is
        // idiomatic multi-line Dart instead of a wall of inline calls.
        // Non-fatal if the toolchain isn't available — we only log.
        val libDir = outDir / "lib"
        if (libDir.exists()) {
          try {
            val rc = sys.process.Process(Seq("dart", "format", libDir.getAbsolutePath)).!
            if (rc != 0) log.warn(s"dart format exited $rc (emission succeeded)")
          } catch {
            case _: java.io.IOException =>
              log.warn("dart not on PATH; skipping auto-format")
          }
        }
      }
    }.value,

    // Platform tasks share scaffold + build logic via helpers below.
    // Each one: emits Dart, scaffolds the platform if missing, runs
    // `flutter build <platform> <extra-args…>`, returns the output path.
    sartLinux := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = baseDirectory.value / "out"
      scaffoldPlatform("linux", outDir, log)
      buildPlatform("linux", Seq.empty, outDir, log)
      val bundle = outDir / "build" / "linux" / "x64" / "release" / "bundle" / "sart_example"
      log.info(s"sart: built $bundle")
      bundle
    },

    sartWeb := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = baseDirectory.value / "out"
      scaffoldPlatform("web", outDir, log)
      buildPlatform("web", Seq.empty, outDir, log)
      val bundle = outDir / "build" / "web"
      log.info(s"sart: built web bundle at $bundle")
      bundle
    },

    sartAndroid := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = baseDirectory.value / "out"
      scaffoldPlatform("android", outDir, log)
      // `apk --debug` is the fastest build for proving the Dart output
      // loads. Users who want release APKs or app bundles can invoke
      // `flutter build apk --release` or `appbundle` themselves.
      buildPlatform("apk", Seq("--debug"), outDir, log)
      val apk = outDir / "build" / "app" / "outputs" / "flutter-apk" / "app-debug.apk"
      log.info(s"sart: built Android APK at $apk")
      apk
    },

    // macOS and Windows tasks: the sbt wiring is identical to sartLinux.
    // `flutter build` itself only succeeds on the matching host — on a
    // Linux box `sartMacOS` will fail at build step with a Flutter error,
    // but the scaffolding and emission are cross-platform.
    sartMacOS := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = baseDirectory.value / "out"
      scaffoldPlatform("macos", outDir, log)
      buildPlatform("macos", Seq.empty, outDir, log)
      val bundle = outDir / "build" / "macos" / "Build" / "Products" / "Release"
      log.info(s"sart: built macOS bundle at $bundle")
      bundle
    },

    sartWindows := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = baseDirectory.value / "out"
      scaffoldPlatform("windows", outDir, log)
      buildPlatform("windows", Seq.empty, outDir, log)
      val bundle = outDir / "build" / "windows" / "x64" / "runner" / "Release"
      log.info(s"sart: built Windows bundle at $bundle")
      bundle
    },

    // iOS build: `--no-codesign` produces an unsigned .app we can verify
    // without Apple Developer provisioning. A signed archive for TestFlight
    // or App Store uses `flutter build ipa` — users handle that manually
    // because it needs team IDs, certificates, etc.
    sartIOS := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = baseDirectory.value / "out"
      scaffoldPlatform("ios", outDir, log)
      buildPlatform("ios", Seq("--no-codesign"), outDir, log)
      val bundle = outDir / "build" / "ios" / "iphoneos" / "Runner.app"
      log.info(s"sart: built iOS bundle at $bundle")
      bundle
    },

    // Step 3: convenience — build then exec the binary.
    sartRun := {
      val log    = streams.value.log
      val binary = sartLinux.value
      log.info(s"sart: launching $binary")
      val rc = sys.process.Process(binary.getAbsolutePath).!
      if (rc != 0) sys.error(s"app exited $rc")
    },

    // Golden-file regression gate: the emitted `lib/main.dart` and
    // `pubspec.yaml` must match the checked-in references under
    // `compiler/src/test/resources/expected/`. Any emitter change that
    // alters the Dart output produces a diff the developer has to
    // explicitly accept via `sartGoldenAccept`. That's enough to catch
    // regressions across the breadth of fixtures in `example/`.
    sartGoldenVerify := {
      sartEmit.value
      val outDir    = baseDirectory.value / "out"
      val goldenDir = baseDirectory.value / "compiler" / "src" / "test" / "resources" / "expected"
      // Cover main.dart, pubspec.yaml, AND every stdlib-shim file the
      // emitter wrote into `out/lib/` alongside main.dart. This way a
      // regression in the shim system shows up in the verify step.
      val libPairs = Option(outDir./("lib").listFiles())
        .getOrElse(Array.empty[File])
        .toSeq
        .filter(_.getName.endsWith(".dart"))
        .map(f => f -> (goldenDir / f.getName))
      val pairs = libPairs :+ (outDir / "pubspec.yaml" -> goldenDir / "pubspec.yaml")
      val drifts = pairs.flatMap { case (actual, expected) =>
        val a = IO.read(actual)
        val e = if (expected.exists()) IO.read(expected) else ""
        if (a == e) None
        else Some {
          // Shell out to `diff -u` for a familiar unified diff. `diff`
          // returns 1 when files differ, so capture stdout via
          // `ProcessLogger` rather than `.!!` (which would throw on the
          // non-zero exit we expect here).
          val sb  = new StringBuilder
          val log = scala.sys.process.ProcessLogger(
            line => { sb.append(line); sb.append('\n') },
            _ => ()
          )
          scala.sys.process.Process(
            Seq("diff", "-u", expected.getAbsolutePath, actual.getAbsolutePath)
          ).!(log)
          s"--- drift in ${actual.getName} ---\n${sb.toString}"
        }
      }
      if (drifts.nonEmpty) {
        drifts.foreach(streams.value.log.error(_))
        sys.error(
          "Emitted Dart drifted from golden files. " +
            "Review the diffs above; if intentional, run `sartGoldenAccept`."
        )
      } else {
        streams.value.log.info("sart: golden files match current emission")
      }
    },

    sartGoldenAccept := {
      sartEmit.value
      val outDir    = baseDirectory.value / "out"
      val goldenDir = baseDirectory.value / "compiler" / "src" / "test" / "resources" / "expected"
      IO.createDirectory(goldenDir)
      // Copy every .dart file under lib/ (main.dart + any shims) plus
      // pubspec.yaml into the golden directory.
      val libFiles = Option(outDir./("lib").listFiles())
        .getOrElse(Array.empty[File])
        .toSeq
        .filter(_.getName.endsWith(".dart"))
      libFiles.foreach(f => IO.copyFile(f, goldenDir / f.getName))
      IO.copyFile(outDir / "pubspec.yaml", goldenDir / "pubspec.yaml")
      streams.value.log.info(s"sart: updated golden files under $goldenDir")
    },

    // `sartAnalyze` — run `flutter analyze lib/` on the emitted output,
    // parse each reported issue, and rewrite the location to the Scala
    // source file that produced the offending Dart class. The mapping
    // uses the `/// Source:` dartdoc comments the emitter prepends to
    // every top-level and method, falling back to the Dart location if
    // no preceding comment is found.
    sartAnalyze := Def.taskDyn {
      sartEmit.value
      Def.task { runAnalyzeAndRemap(baseDirectory.value / "out", streams.value.log) }
    }.value,

    sartAnalyzeOnly := {
      runAnalyzeAndRemap(baseDirectory.value / "out", streams.value.log)
    },

    // `sartWatch` is deliberately trivial — the actual watch mode is sbt's
    // own `~` prefix (`sbt ~sartEmit`). This task exists so `sbt sartWatch`
    // prints the usage hint; everyone types `sartWatch` first.
    sartWatch := {
      streams.value.log.info(
        "sart: use `sbt ~sartEmit` for watch mode. " +
          "In another terminal, run `flutter run -d linux` against out/ and " +
          "press `r` to hot-reload each time sartEmit completes."
      )
    },

    // Single-command bootstrap: publish the 4 core modules plus the
    // separate `sbt-sart/` autoplugin build. First-time setup for any
    // consumer project that wants to `enablePlugins(SartPlugin)`.
    sartPublishLocalAll := {
      val log = streams.value.log
      (`sart-dart` / publishLocal).value
      (`sart-stdlib` / publishLocal).value
      (`flutter-facades` / publishLocal).value
      (compiler / publishLocal).value

      // sbt-sart/ is its own sbt build (Scala 2.12 plugin) — shell out
      // to a subprocess because we can't reach it via project refs.
      val pluginDir = baseDirectory.value / "sbt-sart"
      log.info(s"sart: publishing sbt-sart plugin from $pluginDir")
      val rc = sys.process.Process(
        Seq("sbt", "-Dsbt.color=false", "publishLocal"),
        pluginDir
      ).!
      if (rc != 0) sys.error(s"sbt-sart publishLocal exited $rc")
      log.info("sart: all Sart artifacts published to ~/.ivy2/local/com.outr/")
    }
  )

// Shared between sartAnalyze (which emits first) and any ad-hoc use that
// wants to analyze an already-emitted `out/` without re-running the
// compiler. Separating the logic also keeps the sbt task small.
def runAnalyzeAndRemap(outDir: File, log: Logger): Unit = {
  val libDir = outDir / "lib"
  if (!libDir.exists()) sys.error(s"sartAnalyze: no lib/ at $libDir; nothing to check")

  val rawOutput = new StringBuilder
  val logger = sys.process.ProcessLogger(
    l => { rawOutput.append(l); rawOutput.append('\n') },
    l => { rawOutput.append(l); rawOutput.append('\n') }
  )
  sys.process.Process(
    Seq("flutter", "analyze", "lib/", "--suppress-analytics"),
    outDir
  ).!(logger)

  val IssueRe =
    """^\s*(error|warning|info)\s*•\s*(.+?)\s*•\s*(lib/[^:]+):(\d+):(\d+)\s*•.*$""".r
  rawOutput.toString.linesIterator.foreach { line =>
    line match {
      case IssueRe(sev, msg, dartPath, lineStr, _) =>
        val dartFile = outDir / dartPath
        val dartLine = lineStr.toInt
        mapToScala(dartFile, dartLine) match {
          case Some(sm) => log.info(s"$sev at $sm (Dart: $dartPath:$lineStr) — $msg")
          case None     => log.info(line)
        }
      case other => log.info(other)
    }
  }
}

// ─── Platform-build helpers ───────────────────────────────────────────────

/** Scaffold `flutter create --platforms=<platform>` into `outDir` if the
 *  platform's subdirectory isn't already there, then tidy the default
 *  generated files that don't match Sart's emission (widget_test.dart +
 *  analysis_options.yaml). Idempotent.
 */
def scaffoldPlatform(platform: String, outDir: File, log: Logger): Unit = {
  // `flutter create --platforms=X` adds platform X when missing but
  // never removes or clobbers existing user files. We guard on the
  // directory existing because repeated invocations are still measurable.
  val platformDir = platform match {
    case "linux" | "windows" | "macos" | "android" | "ios" => outDir / platform
    case "web"   => outDir / "web"
    case other   => outDir / other
  }
  if (!platformDir.exists()) {
    log.info(s"sart: scaffolding Flutter $platform platform")
    val rc = sys.process.Process(
      Seq("flutter", "create", s"--platforms=$platform",
          "--project-name=sart_example", "--org", "com.outr",
          "--suppress-analytics", "."),
      outDir
    ).!
    if (rc != 0) sys.error(s"flutter create --platforms=$platform exited $rc")
    tidyScaffold(outDir)
  }
}

/** `flutter create` writes some files that don't match our generated
 *  pubspec or emission assumptions. Replace them with minimal
 *  Sart-compatible versions so `flutter analyze .` stays clean and
 *  `flutter test` doesn't fail on stale scaffolded tests.
 */
def tidyScaffold(outDir: File): Unit = {
  val testFile = outDir / "test" / "widget_test.dart"
  if (testFile.exists()) {
    IO.write(testFile,
      """// Generated by Sart: replaces flutter-create's stale widget_test.
        |// Regenerate by deleting this file and running sartLinux/sartWeb/etc.
        |void main() {}
        |""".stripMargin)
  }
  val analysisFile = outDir / "analysis_options.yaml"
  if (analysisFile.exists()) {
    IO.write(analysisFile,
      """# Generated by Sart — minimal analysis_options for a Sart-produced
        |# Flutter project. Add lint packages via @DartPubspec in your
        |# facades if you want stricter lints.
        |analyzer:
        |  exclude:
        |    - "**/*.g.dart"
        |""".stripMargin)
  }
}

/** Run `flutter build <target> <extraArgs…>` in `outDir`. */
def buildPlatform(target: String, extraArgs: Seq[String], outDir: File, log: Logger): Unit = {
  log.info(s"sart: flutter build $target ${extraArgs.mkString(" ")}")
  val rc = sys.process.Process(
    Seq("flutter", "build", target, "--suppress-analytics") ++ extraArgs,
    outDir
  ).!
  if (rc != 0) sys.error(s"flutter build $target exited $rc")
}

// Tiny helper used by `sartAnalyze`: find the nearest `/// Source:`
// comment at or before `dartLine` in `dartFile`, and compute the Scala
// line as `sourceLine + offset`, where `offset` is the Dart-side distance
// from the comment to the error line.
def mapToScala(dartFile: File, dartLine: Int): Option[String] = {
  if (!dartFile.exists()) return None
  val lines    = IO.read(dartFile).linesIterator.toIndexedSeq
  val SourceRe = """^\s*///\s*Source:\s*(.+?):(\d+)\s*$""".r
  var idx = math.min(dartLine, lines.size) - 1
  while (idx >= 0) {
    lines(idx) match {
      case SourceRe(file, startLine) =>
        val offset = dartLine - (idx + 2)  // comment is at idx+1, next line idx+2
        val scalaLine = startLine.toInt + math.max(0, offset)
        return Some(s"$file:~$scalaLine")
      case _ => idx -= 1
    }
  }
  None
}
