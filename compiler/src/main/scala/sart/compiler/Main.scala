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
 *      produces a `/* TODO … */` comment in the output. For CI gates.
 */
object Main:
  def main(args: Array[String]): Unit =
    val (flags, positional) = args.toList.partition(_.startsWith("--"))
    val strict = flags.contains("--strict")

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

    val tastyFiles = findTastyFiles(exampleClasses)
    if tastyFiles.isEmpty then
      System.err.println(s"No .tasty files found under $exampleClasses")
      System.exit(1)

    val emitter = new DartEmitter(outDir, sourceRoot, projectName, projectDesc)
    val ok = TastyInspector.inspectAllTastyFiles(tastyFiles, Nil, classpath)(emitter)
    if !ok then
      System.err.println("TASTy inspection failed")
      System.exit(1)

    emitter.writeOutput()
    println(s"Wrote Dart output to ${outDir.toAbsolutePath}")

    if strict then
      // Strict mode: any `/* TODO ... */` in the output is a failure. CI
      // gate for surfacing regressions in tree coverage before they ship.
      val libDir = outDir.resolve("lib")
      if Files.exists(libDir) then
        val stream = Files.walk(libDir)
        try
          val todoLines = stream.iterator().asScala
            .filter(p => p.toString.endsWith(".dart"))
            .flatMap { p =>
              val lines = Files.readAllLines(p).asScala.zipWithIndex
              lines.filter(_._1.contains("/* TODO")).map((line, idx) =>
                s"$p:${idx + 1}: $line")
            }.toList
          if todoLines.nonEmpty then
            System.err.println("sart.compiler.Main --strict: unhandled trees in output:")
            todoLines.foreach(l => System.err.println(s"  $l"))
            System.exit(3)
        finally stream.close()

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
