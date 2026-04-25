package sbt.sart

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import java.nio.file.Files

/** Autoplugin exposing the Sart Scala 3 → Dart pipeline.
 *
 *  Minimum setup:
 *    1. Enable the plugin on a Scala 3 project that contains the user's
 *       Sart-authored code:
 *         `enablePlugins(SartPlugin)`
 *    2. Point `sartCompilerJar` at a locally-built `sart-compiler` jar
 *       (e.g. from `sbt publishLocal` of the main repo) and list the
 *       Sart runtime + facade jars in `sartFacadeClasspath`.
 *    3. Run `sbt sartLinux` (or `sartRun`) to build/launch a Flutter
 *       desktop app from your Scala source.
 *
 *  Every task is a thin wrapper around a JVM subprocess running
 *  `sart.compiler.Main`, deliberately keeping this plugin Scala 2.12
 *  / sbt 1.x compatible while the compiler itself runs on Scala 3.
 */
object SartPlugin extends AutoPlugin {
  override def requires: Plugins = JvmPlugin
  override def trigger: PluginTrigger = noTrigger

  // Hidden Ivy configurations that carry Sart's compile-time and facade
  // artifacts separately from the user's main classpath. Keeps the Sart
  // compiler off the user's compile/runtime path — it's only used by the
  // `sartEmit` task via a forked JVM.
  private val SartCompile = config("sartCompile").hide
  private val SartFacade  = config("sartFacade").hide

  object autoImport {
    // ── Settings ────────────────────────────────────────────────────────

    val sartVersion = settingKey[String](
      "Version of the Sart core artifacts (sart-compiler, sart-dart, " +
        "sart-stdlib, flutter-facades) to resolve. Defaults to the " +
        "version this plugin was published at."
    )

    val sartCompilerClasspath = taskKey[Seq[File]](
      "Classpath that runs sart.compiler.Main in a forked JVM. Auto-populated " +
        "from sartVersion; override to supply custom jars."
    )

    val sartFacadeClasspath = taskKey[Seq[File]](
      "Classpath entries carrying Sart facade TASTy: sart-dart, sart-stdlib, " +
        "flutter-facades (plus any user-authored facades). Auto-populated."
    )

    val sartOutDir = settingKey[File](
      "Sart output directory (default: <base>/out)."
    )

    val sartSourceRoot = settingKey[File](
      "Base directory used to relativise /// Source: attribution comments " +
        "(default: baseDirectory of the enabling project)."
    )

    val sartGoldenDir = settingKey[File](
      "Directory holding checked-in golden Dart for sartGoldenVerify / Accept."
    )

    // ── Tasks ──────────────────────────────────────────────────────────

    val sartEmit          = taskKey[Unit]("Compile this project and emit Dart into <sartOutDir>/lib/")
    val sartLinux         = taskKey[File]("Build a native Linux bundle from the emitted Dart")
    val sartWeb           = taskKey[File]("Build a Flutter web bundle from the emitted Dart")
    val sartAndroid       = taskKey[File]("Build a Flutter Android debug APK from the emitted Dart")
    val sartMacOS         = taskKey[File]("Build a Flutter macOS bundle (requires macOS host)")
    val sartWindows       = taskKey[File]("Build a Flutter Windows bundle (requires Windows host)")
    val sartIOS           = taskKey[File]("Build a Flutter iOS bundle — no-codesign (requires macOS + Xcode)")
    val sartRun           = taskKey[Unit]("Build and launch the generated Linux app")
    val sartGoldenVerify  = taskKey[Unit]("Emit Dart and diff it against sartGoldenDir")
    val sartGoldenAccept  = taskKey[Unit]("Emit Dart and overwrite sartGoldenDir with the new output")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    // Sart needs TASTy trees retained for the inspector to walk.
    Compile / scalacOptions += "-Yretain-trees",

    sartVersion    := "0.1.0-SNAPSHOT",
    sartOutDir     := baseDirectory.value / "out",
    sartSourceRoot := baseDirectory.value,
    sartGoldenDir  := baseDirectory.value / "sart-golden",

    // Register hidden Ivy configurations so the Sart jars resolve via
    // `update` without polluting the user's main classpath. Users still
    // need the Scala 3 sart modules reachable from their compile path
    // for TASTy typechecking, so we *also* add facade artifacts to the
    // default `libraryDependencies`.
    ivyConfigurations ++= Seq(SartCompile, SartFacade),
    libraryDependencies ++= Seq(
      "com.outr" %% "sart-compiler"   % sartVersion.value % SartCompile,
      "com.outr" %% "sart-dart"       % sartVersion.value % SartFacade,
      "com.outr" %% "sart-stdlib"     % sartVersion.value % SartFacade,
      "com.outr" %% "flutter-facades" % sartVersion.value % SartFacade,
      // And on the user's default classpath so their Scala code compiles.
      "com.outr" %% "sart-dart"       % sartVersion.value,
      "com.outr" %% "sart-stdlib"     % sartVersion.value,
      "com.outr" %% "flutter-facades" % sartVersion.value
    ),
    sartCompilerClasspath := Classpaths.managedJars(SartCompile, classpathTypes.value, update.value).map(_.data),
    sartFacadeClasspath   := Classpaths.managedJars(SartFacade,  classpathTypes.value, update.value).map(_.data),

    // ── sartEmit: run the Scala 3 compiler to TASTy, then Sart → Dart ───

    sartEmit := {
      val log       = streams.value.log
      (Compile / compile).value
      val exClasses = (Compile / classDirectory).value
      val userCp    = (Compile / fullClasspath).value.map(_.data)
      val compilerCp = sartCompilerClasspath.value
      val facadeCp   = sartFacadeClasspath.value
      val outDir     = sartOutDir.value
      val sourceRoot = sartSourceRoot.value

      if (compilerCp.isEmpty)
        sys.error("sbt-sart: sartCompilerClasspath is empty. Set it to the jars that run sart.compiler.Main.")

      IO.createDirectory(outDir)

      // The classpath the inspector uses to resolve references — must
      // include facade jars plus the user's compile output.
      val inspectorCp = (facadeCp ++ userCp).distinct
        .map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)

      // The classpath of the JVM we fork: the compiler jar + its deps.
      val runCp = compilerCp.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)

      // Dart project name: a pubspec identifier, so it has to be
      // lowercase + underscores. Sbt's `normalizedName` is already the
      // kebab-case project name; swap dashes for underscores.
      val pubName = normalizedName.value.replace('-', '_')
      val pubDesc = description.value

      val args = Seq(
        exClasses.getAbsolutePath,
        inspectorCp,
        outDir.getAbsolutePath,
        sourceRoot.getAbsolutePath,
        pubName,
        pubDesc
      )

      log.info(s"sbt-sart: emitting Dart into $outDir")
      val rc = sys.process.Process(
        Seq(
          "java", "-cp", runCp,
          "sart.compiler.Main"
        ) ++ args
      ).!
      if (rc != 0) sys.error(s"sart.compiler.Main exited $rc")

      // Run dart format on the lib/ dir if dart is available. Non-fatal.
      // Language version is discovered from the stub
      // `.dart_tool/package_config.json` the emitter writes alongside
      // pubspec.yaml, so output is identical regardless of whether
      // `flutter pub get` has run.
      val libDir = outDir / "lib"
      if (libDir.exists()) {
        try {
          sys.process.Process(Seq("dart", "format", libDir.getAbsolutePath)).!
        } catch {
          case _: java.io.IOException =>
            log.warn("dart not on PATH; skipping auto-format")
        }
      }
    },

    // ── Flutter build targets ────────────────────────────────────────────

    sartLinux := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = sartOutDir.value
      sbtSartScaffold("linux", outDir, normalizedName.value, log)
      sbtSartBuild("linux", Seq.empty, outDir, log)
      val bundleDir = outDir / "build" / "linux" / "x64" / "release" / "bundle"
      val binary    = Option(bundleDir.listFiles()).getOrElse(Array.empty)
        .find(f => f.canExecute && !f.isDirectory)
        .getOrElse(sys.error(s"sbt-sart: no bundled binary found in $bundleDir"))
      log.info(s"sbt-sart: built $binary")
      binary
    },

    sartWeb := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = sartOutDir.value
      sbtSartScaffold("web", outDir, normalizedName.value, log)
      // `-Dsart.web.baseHref=/foo/` lets consumers pin the asset root
      // for project-site deploys (e.g. GitHub Pages under a subpath).
      val extra = sys.props.get("sart.web.baseHref").filter(_.nonEmpty)
        .map(h => Seq(s"--base-href=$h")).getOrElse(Seq.empty)
      sbtSartBuild("web", extra, outDir, log)
      val bundle = outDir / "build" / "web"
      log.info(s"sbt-sart: built web bundle at $bundle")
      bundle
    },

    sartAndroid := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = sartOutDir.value
      sbtSartScaffold("android", outDir, normalizedName.value, log)
      sbtSartBuild("apk", Seq("--debug"), outDir, log)
      val apk = outDir / "build" / "app" / "outputs" / "flutter-apk" / "app-debug.apk"
      log.info(s"sbt-sart: built Android APK at $apk")
      apk
    },

    sartMacOS := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = sartOutDir.value
      sbtSartScaffold("macos", outDir, normalizedName.value, log)
      sbtSartBuild("macos", Seq.empty, outDir, log)
      val bundle = outDir / "build" / "macos" / "Build" / "Products" / "Release"
      log.info(s"sbt-sart: built macOS bundle at $bundle")
      bundle
    },

    sartWindows := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = sartOutDir.value
      sbtSartScaffold("windows", outDir, normalizedName.value, log)
      sbtSartBuild("windows", Seq.empty, outDir, log)
      val bundle = outDir / "build" / "windows" / "x64" / "runner" / "Release"
      log.info(s"sbt-sart: built Windows bundle at $bundle")
      bundle
    },

    sartIOS := {
      val log    = streams.value.log
      sartEmit.value
      val outDir = sartOutDir.value
      sbtSartScaffold("ios", outDir, normalizedName.value, log)
      // `--no-codesign` builds the .app without requiring Apple provisioning.
      // For signed builds (TestFlight/App Store), users should invoke
      // `flutter build ipa` with their own signing configuration.
      sbtSartBuild("ios", Seq("--no-codesign"), outDir, log)
      val bundle = outDir / "build" / "ios" / "iphoneos" / "Runner.app"
      log.info(s"sbt-sart: built iOS bundle at $bundle")
      bundle
    },

    sartRun := {
      val binary = sartLinux.value
      val rc = sys.process.Process(binary.getAbsolutePath).!
      if (rc != 0) sys.error(s"app exited $rc")
    },

    // ── Golden-file gates ────────────────────────────────────────────────

    sartGoldenVerify := {
      sartEmit.value
      val log       = streams.value.log
      val outDir    = sartOutDir.value
      val goldenDir = sartGoldenDir.value
      val libFiles = Option((outDir / "lib").listFiles()).getOrElse(Array.empty)
        .toSeq.filter(_.getName.endsWith(".dart"))
      val pairs = libFiles.map(f => f -> (goldenDir / f.getName)) :+
        ((outDir / "pubspec.yaml") -> (goldenDir / "pubspec.yaml"))
      val drifts = pairs.flatMap { case (actual, expected) =>
        val a = IO.read(actual)
        val e = if (expected.exists()) IO.read(expected) else ""
        if (a == e) None
        else Some {
          val sb = new StringBuilder
          val lg = sys.process.ProcessLogger(l => { sb.append(l); sb.append('\n') }, _ => ())
          sys.process.Process(
            Seq("diff", "-u", expected.getAbsolutePath, actual.getAbsolutePath)
          ).!(lg)
          s"--- drift in ${actual.getName} ---\n${sb.toString}"
        }
      }
      if (drifts.nonEmpty) {
        drifts.foreach(log.error(_))
        sys.error("Emitted Dart drifted from golden files. If intentional, run `sartGoldenAccept`.")
      } else {
        log.info("sbt-sart: golden files match current emission")
      }
    },

    sartGoldenAccept := {
      sartEmit.value
      val outDir    = sartOutDir.value
      val goldenDir = sartGoldenDir.value
      IO.createDirectory(goldenDir)
      val libFiles = Option((outDir / "lib").listFiles()).getOrElse(Array.empty)
        .toSeq.filter(_.getName.endsWith(".dart"))
      libFiles.foreach(f => IO.copyFile(f, goldenDir / f.getName))
      IO.copyFile(outDir / "pubspec.yaml", goldenDir / "pubspec.yaml")
      streams.value.log.info(s"sbt-sart: updated golden files under $goldenDir")
    }
  )

  // ── Private helpers shared by the platform tasks ───────────────────────

  /** Idempotent `flutter create --platforms=<platform> .` in `outDir`,
   *  plus the widget_test.dart + analysis_options.yaml tidy-up that
   *  keeps `flutter analyze .` quiet on Sart-generated projects.
   */
  private def sbtSartScaffold(
    platform: String, outDir: File, projectName: String, log: Logger
  ): Unit = {
    val platformDir = platform match {
      case "linux" | "windows" | "macos" | "android" | "ios" => outDir / platform
      case "web"   => outDir / "web"
      case other   => outDir / other
    }
    if (!platformDir.exists()) {
      log.info(s"sbt-sart: scaffolding Flutter $platform platform")
      val projName = projectName.replace('-', '_')
      val rc = sys.process.Process(
        Seq("flutter", "create", s"--platforms=$platform",
            "--project-name", projName,
            "--org", "com.example", "--suppress-analytics", "."),
        outDir
      ).!
      if (rc != 0) sys.error(s"flutter create --platforms=$platform exited $rc")
      val testFile = outDir / "test" / "widget_test.dart"
      if (testFile.exists()) IO.write(testFile, "void main() {}\n")
      val analysisFile = outDir / "analysis_options.yaml"
      if (analysisFile.exists()) IO.write(analysisFile,
        "analyzer:\n  exclude:\n    - \"**/*.g.dart\"\n")
    }
  }

  private def sbtSartBuild(
    target: String, extraArgs: Seq[String], outDir: File, log: Logger
  ): Unit = {
    log.info(s"sbt-sart: flutter build $target ${extraArgs.mkString(" ")}")
    val rc = sys.process.Process(
      Seq("flutter", "build", target, "--suppress-analytics") ++ extraArgs,
      outDir
    ).!
    if (rc != 0) sys.error(s"flutter build $target exited $rc")
  }
}
