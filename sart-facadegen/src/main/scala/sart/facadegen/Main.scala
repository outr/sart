package sart.facadegen

import java.nio.file.{Files, Path, Paths}
import java.nio.charset.StandardCharsets
import scala.sys.process.{Process, ProcessLogger}

/** Sart facade-generator CLI.
 *
 *  Usage: `sart.facadegen.Main <dart-file-or-dir> <output-dir> [<dart-import-path>]`
 *
 *  - Invokes the Dart helper in `tool/bin/facadegen.dart` to parse the
 *    input Dart source and emit a JSON description of its public API.
 *  - Parses the JSON and writes Scala `.scala` facade files into the
 *    output directory.
 *  - `dart-import-path` is the `package:…/…` string that Sart users will
 *    see as `@DartImport(…)` on the emitted facades. If omitted, it's
 *    inferred from the input file name.
 */
object Main:

  // The tool subdirectory containing the Dart helper. Allow override via
  // SART_FACADEGEN_TOOL env var; otherwise look relative to the current
  // working dir (which sbt sets per-module on fork) AND relative to the
  // JVM's working dir, walking up if needed.
  private val toolRoot: Path =
    Option(System.getenv("SART_FACADEGEN_TOOL")).map(Paths.get(_).toAbsolutePath)
      .orElse(findToolRoot(Paths.get(".").toAbsolutePath))
      .getOrElse(Paths.get("sart-facadegen/tool").toAbsolutePath)

  private def findToolRoot(start: Path): Option[Path] =
    var cur: Path = start.toAbsolutePath.normalize
    while cur != null do
      val candidate = cur.resolve("sart-facadegen/tool")
      if Files.isDirectory(candidate) then return Some(candidate)
      val self = cur.resolve("tool")
      if Files.isDirectory(self) && cur.getFileName != null && cur.getFileName.toString == "sart-facadegen" then
        return Some(self)
      cur = cur.getParent
    None

  def main(args: Array[String]): Unit =
    if args.length < 2 then
      System.err.println(
        "usage: sart.facadegen.Main <dart-file-or-dir> <output-dir> [<dart-import-path>]"
      )
      System.exit(2)

    val input     = Paths.get(args(0)).toAbsolutePath
    val outputDir = Paths.get(args(1)).toAbsolutePath
    val importPath = if args.length >= 3 then args(2)
                     else s"package:${input.getFileName}"

    Files.createDirectories(outputDir)

    val dartFiles =
      if Files.isDirectory(input) then
        collectDartFiles(input)
      else
        List(input)

    for dartFile <- dartFiles do
      val json = runDartHelper(dartFile)
      val lib  = JsonParser.parse(json)
      val scalaSource = FacadeWriter.render(lib, importPath)
      val outFile = outputDir.resolve(dartFile.getFileName.toString.replace(".dart", ".scala"))
      Files.writeString(outFile, scalaSource, StandardCharsets.UTF_8)
      println(s"sart-facadegen: wrote $outFile")

  private def collectDartFiles(dir: Path): List[Path] =
    val stream = Files.walk(dir)
    try
      import scala.jdk.CollectionConverters.*
      stream.iterator().asScala
        .filter(p => p.toString.endsWith(".dart"))
        .toList.sorted
    finally stream.close()

  /** Run the Dart-side helper as a subprocess, capturing its stdout. Uses
   *  the user-provided path from the `SART_DART` env var if present, or
   *  looks on PATH; falls back to the Flutter-SDK-bundled location that
   *  ships on most dev machines.
   */
  private val dartBinary: String =
    Option(System.getenv("SART_DART"))
      .orElse(Option(System.getenv("PATH")).flatMap(findOnPath("dart", _)))
      .getOrElse("/home/mhicks/opt/flutter/bin/dart") // fall-through for dev

  private def findOnPath(exe: String, path: String): Option[String] =
    path.split(java.io.File.pathSeparator).iterator
      .map(dir => Paths.get(dir, exe))
      .find(p => Files.isExecutable(p))
      .map(_.toString)

  private def runDartHelper(dartFile: Path): String =
    val stdout = new StringBuilder
    val stderr = new StringBuilder
    val logger = ProcessLogger(
      l => { stdout.append(l); stdout.append('\n') },
      l => { stderr.append(l); stderr.append('\n') }
    )
    val cmd = Seq(
      dartBinary, "run",
      "--suppress-analytics",
      toolRoot.resolve("bin/facadegen.dart").toString,
      dartFile.toString
    )
    val rc = Process(cmd, toolRoot.toFile).!(logger)
    if rc != 0 then
      System.err.println(s"Dart helper failed (rc=$rc) on $dartFile:\n${stderr.toString}")
      sys.exit(rc)
    stdout.toString
