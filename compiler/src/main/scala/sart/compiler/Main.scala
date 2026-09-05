package sart.compiler

import scala.tasty.inspector.TastyInspector
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*

/** CLI entry for the Sart compiler.
 *
 *  Positional args (required first, then optional — same order as before):
 *    1. `<example-classes-dir>`: TASTy root for the user's code.
 *    2. `<classpath>`: path-separator-joined inspector classpath.
 *    3. `<out-dir>`: where to write `lib/main.dart` + `pubspec.yaml`.
 *    4. `<source-root>` (optional): base dir for `/// Source:` comments.
 *    5. `<project-name>` (optional): Dart pubspec name.
 *    6. `<project-description>` (optional): Dart pubspec description.
 *
 *  Flag args (anywhere):
 *    - `--strict`: fail with a non-zero exit when any unhandled tree
 *      produces a `/* TODO … */` comment in the output, reporting each
 *      gap at its Scala source location. For CI gates.
 *    - `--library=<jar-or-classes-dir>` (repeatable): also emit the TASTy
 *      found in a dependency — a jar's `.tasty` entries or a sibling
 *      project's classes directory — so shared modules and Sart-aware
 *      libraries compile through with the app.
 */
object Main:
  def main(args: Array[String]): Unit =
    val (flags, positional) = args.toList.partition(_.startsWith("--"))
    val strict = flags.contains("--strict")
    val libraries = flags.collect { case f if f.startsWith("--library=") => f.stripPrefix("--library=") }
    val wireMappings = flags.collect { case f if f.startsWith("--wire-mapping=") => f.stripPrefix("--wire-mapping=") }
      .flatMap { m => m.split("=", 2) match { case Array(a, b) => Some(a -> b); case _ => None } }.toMap

    if positional.length < 3 || positional.length > 6 then
      System.err.println(
        "usage: sart.compiler.Main [--strict] <example-classes-dir> <classpath> <out-dir> " +
          "[<source-root> [<project-name> [<project-description>]]]"
      )
      System.exit(2)

    val exampleClasses = Paths.get(positional(0))
    val classpath      = positional(1).split(java.io.File.pathSeparator).toList.filter(_.nonEmpty)
    val outDir         = Paths.get(positional(2))
    val sourceRoot     = if positional.length >= 4 then Some(Paths.get(positional(3)).toAbsolutePath) else None
    val projectName    = if positional.length >= 5 then positional(4) else "sart_example"
    val projectDesc    = if positional.length >= 6 then positional(5) else "Scala-authored Flutter app, compiled via Sart."

    val ownTasty = findTastyFiles(exampleClasses)
    if ownTasty.isEmpty then
      System.err.println(s"No .tasty files found under $exampleClasses")
      System.exit(1)
    // Dependency compile-through: classes directories contribute their
    // .tasty files alongside the app's; jars go to the inspector as jars.
    val (libDirs, libJars) = libraries.map(Paths.get(_)).partition(Files.isDirectory(_))
    val tastyFiles = (ownTasty ++ libDirs.flatMap(findTastyFiles)).distinct
    val jars = libJars.map(_.toString)
    if libraries.nonEmpty then
      println(s"Compiling through ${libJars.size} library jar(s) and ${libDirs.size} project dir(s)")

    val emitter = new DartEmitter(outDir, sourceRoot, projectName, projectDesc, wireMappings)
    val ok = TastyInspector.inspectAllTastyFiles(tastyFiles, jars, classpath)(emitter)
    if !ok then
      System.err.println("TASTy inspection failed")
      System.exit(1)

    emitter.writeOutput()
    println(s"Wrote Dart output to ${outDir.toAbsolutePath}")

    if strict && emitter.unsupported.nonEmpty then
      // Strict mode: every unsupported tree is a build failure, reported
      // at the Scala member it came from — a compile error, not a Dart
      // analyzer surprise later.
      System.err.println(s"sart: --strict: ${emitter.unsupported.size} unsupported construct(s):")
      emitter.unsupported.foreach(g => System.err.println(s"  $g"))
      System.exit(3)

  private def findTastyFiles(root: Path): List[String] =
    if !Files.exists(root) then Nil
    else
      val stream = Files.walk(root)
      try
        stream.iterator().asScala
          .filter(p => p.toString.endsWith(".tasty"))
          .map(_.toString)
          // Sort for deterministic emission order — golden-file tests
          // depend on stable output across runs and machines.
          .toList.sorted
      finally stream.close()
