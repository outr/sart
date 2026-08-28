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

  // Extract `--language-version=<major.minor>` from the SDK lower bound
  // in the emitted pubspec, so `dart format` produces deterministic
  // output regardless of which Dart version is on PATH or whether
  // `pub get` has populated `.dart_tool/package_config.json`.
  private[sart] def dartLanguageArgs(pubspec: File): Seq[String] = {
    if (!pubspec.exists()) Seq.empty
    else {
      val sdkRe = """sdk:\s*['"]?>=(\d+\.\d+)""".r
      sdkRe.findFirstMatchIn(IO.read(pubspec))
        .map(m => Seq(s"--language-version=${m.group(1)}"))
        .getOrElse(Seq.empty)
    }
  }

  // Java's ProcessBuilder doesn't resolve `.bat`/`.cmd` extensions on
  // Windows, so `Process(Seq("flutter", ...))` fails with CreateProcess
  // error=2 because there's no literal `flutter` binary — only
  // `flutter.bat`. Pick the right name per OS.
  private[sart] val isWindows: Boolean =
    sys.props.get("os.name").exists(_.toLowerCase.contains("windows"))
  private[sart] def flutterCmd: String = if (isWindows) "flutter.bat" else "flutter"
  private[sart] def dartCmd:    String = if (isWindows) "dart.bat"    else "dart"

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

    @transient val sartCompilerClasspath = taskKey[Seq[File]](
      "Classpath that runs sart.compiler.Main in a forked JVM. Auto-populated " +
        "from sartVersion; override to supply custom jars."
    )

    @transient val sartFacadeClasspath = taskKey[Seq[File]](
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

    val sartPubspecLock = settingKey[Option[File]](
      "A pubspec.lock copied into <out>/ on every emit so `flutter pub get` resolves exactly those " +
      "package versions — e.g. the lock of a hand-written app the Sart build must match pixel-for-pixel."
    )
    val sartWebDir = settingKey[Option[File]](
      "Directory whose contents overlay the scaffolded <out>/web/ before `flutter build web` — " +
      "a hand-written index.html, flutter_bootstrap.js, manifest.json, icons/… (like a Flutter app's own web/ folder)."
    )
    val sartAssets = settingKey[Seq[File]](
      "Files or directories bundled as Flutter assets: each entry is copied " +
        "into <sartOutDir>/assets/ on sartEmit (a directory contributes its " +
        "contents). Declare the matching `flutter: assets:` pubspec section " +
        "with @DartPubspec in project code."
    )

    val sartLibraries = settingKey[Seq[ModuleID]](
      "Dependencies whose TASTy is compiled through to Dart along with this " +
        "project — shared model modules and Sart-aware libraries. Matched " +
        "against the resolved classpath by organization + name (cross-version " +
        "suffix ignored). Projects reached via dependsOn are always included."
    )

    val sartStrict = settingKey[Boolean](
      "Fail sartEmit on any Scala construct the emitter can't translate, " +
        "reported at its Scala source location, instead of leaving a " +
        "/* TODO */ in the Dart for the analyzer to trip over later."
    )

    // ── Tasks ──────────────────────────────────────────────────────────

    @transient val sartEmit          = taskKey[Unit]("Compile this project and emit Dart into <sartOutDir>/lib/")
    @transient val sartLinux         = taskKey[File]("Build a native Linux bundle from the emitted Dart")
    @transient val sartWeb           = taskKey[File]("Build a Flutter web bundle from the emitted Dart")
    @transient val sartAndroid       = taskKey[File]("Build a Flutter Android debug APK from the emitted Dart")
    @transient val sartMacOS         = taskKey[File]("Build a Flutter macOS bundle (requires macOS host)")
    @transient val sartWindows       = taskKey[File]("Build a Flutter Windows bundle (requires Windows host)")
    @transient val sartIOS           = taskKey[File]("Build a Flutter iOS bundle — no-codesign (requires macOS + Xcode)")
    @transient val sartRun           = taskKey[Unit]("Build and launch the generated Linux app")
    @transient val sartGoldenVerify  = taskKey[Unit]("Emit Dart and diff it against sartGoldenDir")
    @transient val sartGoldenAccept  = taskKey[Unit]("Emit Dart and overwrite sartGoldenDir with the new output")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    // Sart needs TASTy trees retained for the inspector to walk.
    Compile / scalacOptions += "-Yretain-trees",

    sartVersion    := "0.1.0-SNAPSHOT",
    sartOutDir     := baseDirectory.value / "out",
    sartSourceRoot := baseDirectory.value,
    sartGoldenDir  := baseDirectory.value / "sart-golden",
    sartAssets     := Seq.empty,
    sartWebDir     := None,
    sartPubspecLock := None,
    sartLibraries  := Seq.empty,
    sartStrict     := false,

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
    sartCompilerClasspath := PluginCompat.managedJarFiles(
      SartCompile, classpathTypes.value, update.value, fileConverter.value
    ),
    sartFacadeClasspath := PluginCompat.managedJarFiles(
      SartFacade, classpathTypes.value, update.value, fileConverter.value
    ),

    // ── sartEmit: run the Scala 3 compiler to TASTy, then Sart → Dart ───

    sartEmit := {
      val log       = streams.value.log
      (Compile / compile).value
      val exClasses = (Compile / classDirectory).value
      val userCp    = PluginCompat.toFiles((Compile / fullClasspath).value, fileConverter.value)
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

      // Compile-through: dependsOn projects (their classes dirs) plus any
      // declared library whose resolved jar is on the classpath.
      val wanted = sartLibraries.value
      def matches(m: ModuleID, w: ModuleID): Boolean =
        w.organization == m.organization && (m.name == w.name || m.name.startsWith(w.name + "_"))
      val resolved = PluginCompat.moduleJars(update.value, fileConverter.value)
      val libJarFiles = resolved.collect { case (m, f) if wanted.exists(w => matches(m, w)) && f.getName.endsWith(".jar") => f }.distinct
      wanted.filterNot(w => resolved.exists { case (m, _) => matches(m, w) })
        .foreach(w => log.warn(s"sbt-sart: sartLibraries entry ${w.organization}:${w.name} is not on the compile classpath"))
      // dependsOn projects: classes directories under sbt 1, exported jars under sbt 2 — Main handles both.
      val projectEntries = PluginCompat.toFiles((Compile / internalDependencyClasspath).value, fileConverter.value)
      val libraryArgs = (libJarFiles ++ projectEntries).distinct.map(f => s"--library=${f.getAbsolutePath}")
      if (libraryArgs.nonEmpty)
        log.info(s"sbt-sart: compiling through ${libJarFiles.size} library jar(s), ${projectEntries.size} dependsOn project(s)")
      val strictArgs = if (sartStrict.value) Seq("--strict") else Seq.empty

      val args = strictArgs ++ libraryArgs ++ Seq(
        exClasses.getAbsolutePath,
        inspectorCp,
        outDir.getAbsolutePath,
        sourceRoot.getAbsolutePath,
        pubName,
        pubDesc
      )

      sartPubspecLock.value.foreach { lock =>
        if (!lock.isFile) sys.error(s"sartPubspecLock is not a file: $lock")
        IO.copyFile(lock, outDir / "pubspec.lock")
        log.info(s"sbt-sart: pinned package versions from $lock")
      }
      // Bundle declared assets before the emitter runs so a failed emit
      // never leaves a half-updated assets tree ahead of stale Dart.
      val assets = sartAssets.value
      if (assets.nonEmpty) {
        val assetsDir = outDir / "assets"
        IO.createDirectory(assetsDir)
        assets.foreach { entry =>
          if (entry.isDirectory) IO.copyDirectory(entry, assetsDir)
          else if (entry.isFile) IO.copyFile(entry, assetsDir / entry.getName)
          else log.warn(s"sbt-sart: asset entry does not exist: $entry")
        }
        log.info(s"sbt-sart: bundled ${assets.size} asset entr${if (assets.size == 1) "y" else "ies"} into $assetsDir")
      }

      log.info(s"sbt-sart: emitting Dart into $outDir")
      val rc = sys.process.Process(
        Seq(
          "java", "-cp", runCp,
          "sart.compiler.Main"
        ) ++ args
      ).!
      if (rc != 0) sys.error(s"sart.compiler.Main exited $rc")

      // Run dart format on the lib/ dir if dart is available. Non-fatal.
      // Pin --language-version to the major.minor extracted from the
      // emitted pubspec's SDK floor so output is identical regardless of
      // whether `flutter pub get` has populated package_config.json.
      val libDir = outDir / "lib"
      if (libDir.exists()) {
        try {
          val langArgs = SartPlugin.dartLanguageArgs(outDir / "pubspec.yaml")
          sys.process.Process(Seq(SartPlugin.dartCmd, "format") ++ langArgs :+ libDir.getAbsolutePath).!
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
      val binary    = Option(bundleDir.listFiles()).getOrElse(Array.empty[File])
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
      sartWebDir.value.foreach { dir =>
        if (!dir.isDirectory) sys.error(s"sartWebDir is not a directory: $dir")
        IO.copyDirectory(dir, outDir / "web", overwrite = true, preserveLastModified = true)
        log.info(s"sbt-sart: overlaid web/ from $dir")
      }
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
      val libFiles = Option((outDir / "lib").listFiles()).getOrElse(Array.empty[File])
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
      val libFiles = Option((outDir / "lib").listFiles()).getOrElse(Array.empty[File])
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
        Seq(SartPlugin.flutterCmd, "create", s"--platforms=$platform",
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
      Seq(SartPlugin.flutterCmd, "build", target, "--suppress-analytics") ++ extraArgs,
      outDir
    ).!
    if (rc != 0) sys.error(s"flutter build $target exited $rc")
  }
}
