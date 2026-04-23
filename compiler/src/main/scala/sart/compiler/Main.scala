package sart.compiler

import scala.tasty.inspector.TastyInspector
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*

/** CLI entry for the Sart compiler.
 *
 *  Usage: `sart.compiler.Main <example-classes-dir> <classpath> <out-dir>`
 *
 *  - `example-classes-dir`: directory containing the user code's `.tasty`
 *    files (we emit Dart for everything we find under here).
 *  - `classpath`: `File.pathSeparator`-joined list of classpath entries that
 *    the TASTy inspector needs to resolve references (facade module,
 *    scala3-library, etc.).
 *  - `out-dir`: directory to write `lib/main.dart` and `pubspec.yaml` into.
 */
object Main:
  def main(args: Array[String]): Unit =
    if args.length != 3 then
      System.err.println(
        "usage: sart.compiler.Main <example-classes-dir> <classpath> <out-dir>"
      )
      System.exit(2)

    val exampleClasses = Paths.get(args(0))
    val classpath      = args(1).split(java.io.File.pathSeparator).toList.filter(_.nonEmpty)
    val outDir         = Paths.get(args(2))

    val tastyFiles = findTastyFiles(exampleClasses)
    if tastyFiles.isEmpty then
      System.err.println(s"No .tasty files found under $exampleClasses")
      System.exit(1)

    val emitter = new DartEmitter(outDir)
    val ok = TastyInspector.inspectAllTastyFiles(tastyFiles, Nil, classpath)(emitter)
    if !ok then
      System.err.println("TASTy inspection failed")
      System.exit(1)

    emitter.writeOutput()
    println(s"Wrote Dart output to ${outDir.toAbsolutePath}")

  private def findTastyFiles(root: Path): List[String] =
    if !Files.exists(root) then Nil
    else
      val stream = Files.walk(root)
      try
        stream.iterator().asScala
          .filter(p => p.toString.endsWith(".tasty"))
          .map(_.toString)
          .toList
      finally stream.close()
