package sart.facadegen

import java.nio.file.{Files, Path, Paths}
import java.nio.charset.StandardCharsets
import scala.sys.process.{Process, ProcessLogger}
import scala.jdk.CollectionConverters.*

/** Sart facade-generator CLI.
 *
 *  Config mode (the normal gesture — regenerates a facade set):
 *
 *    sart.facadegen.Main --config <facadegen.conf>
 *
 *  Config format (line-oriented; `#` comments):
 *
 *    package flutter.material            # Scala package of the output
 *    import package:flutter/material.dart
 *    out ../src/main/scala/flutter/material_generated.scala
 *    flutter-root /home/me/flutter       # optional; FLUTTER_ROOT env wins
 *    curated Widget StatelessWidget …    # names referenced but not emitted
 *    file packages/flutter/lib/src/material/card.dart
 *    keep Card
 *    file …
 *    keep A B C
 *
 *  Paths under `file` resolve against flutter-root unless absolute; `out`
 *  resolves against the config file's directory. All files are resolved in
 *  a single Dart-helper invocation and rendered into one Scala source file.
 *
 *  Ad-hoc mode (one-off exploration, unchanged from the MVP):
 *
 *    sart.facadegen.Main <dart-file-or-dir> <output-dir> [<dart-import>]
 */
object Main:

  private val toolRoot: Path =
    Option(System.getenv("SART_FACADEGEN_TOOL")).map(Paths.get(_).toAbsolutePath)
      .orElse(findToolRoot(Paths.get(".").toAbsolutePath))
      .getOrElse(sys.error(
        "sart-facadegen: cannot locate the Dart helper (tool/). " +
        "Set SART_FACADEGEN_TOOL to the tool directory."))

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
    args.toList match
      case "--config" :: cfgPath :: Nil => runConfig(Paths.get(cfgPath).toAbsolutePath)
      case input :: outDir :: rest if !input.startsWith("--") =>
        runAdHoc(Paths.get(input).toAbsolutePath, Paths.get(outDir).toAbsolutePath, rest.headOption)
      case _ =>
        System.err.println(
          "usage: sart.facadegen.Main --config <facadegen.conf>\n" +
          "   or: sart.facadegen.Main <dart-file-or-dir> <output-dir> [<dart-import-path>]")
        System.exit(2)

  // ── Config mode ──────────────────────────────────────────────────────

  private case class Config(
    scalaPackage: String,
    dartImport: String,
    out: Path,
    curated: Set[String],
    curatedGeneric: Map[String, Int],
    files: List[(Path, Set[String])]
  )

  private def runConfig(cfgPath: Path): Unit =
    val cfg  = parseConfig(cfgPath)
    val dartFiles = cfg.files.map(_._1)
    dartFiles.filterNot(Files.exists(_)) match
      case Nil     => ()
      case missing => sys.error(s"facadegen: missing input files:\n  ${missing.mkString("\n  ")}")

    System.err.println(s"sart-facadegen: resolving ${dartFiles.size} Dart files (this can take a minute)…")
    val json = runDartHelper(dartFiles)
    val dump = JsonParser.parse(json)

    val keepByFile: Map[String, Set[String]] =
      cfg.files.map((p, keeps) => p.toString -> keeps).toMap

    val rendered = FacadeWriter.render(
      dump,
      FacadeWriter.GenConfig(cfg.scalaPackage, cfg.dartImport, cfg.curated, cfg.curatedGeneric, keepByFile)
    )

    // Warn about requested names that never materialised.
    val emitted = dump.files.flatMap(f => f.classes.map(_.name) ++ f.enums.map(_.name)).toSet
    for
      (p, keeps) <- cfg.files
      k <- keeps if !emitted.contains(k)
    do System.err.println(s"sart-facadegen: WARNING — '$k' not found in ${p.getFileName}")

    Files.createDirectories(cfg.out.getParent)
    Files.writeString(cfg.out, rendered, StandardCharsets.UTF_8)
    println(s"sart-facadegen: wrote ${cfg.out}")

  private def parseConfig(cfgPath: Path): Config =
    val flutterRoot = Option(System.getenv("FLUTTER_ROOT")).map(Paths.get(_))
    var pkg = "generated"
    var imp = "package:flutter/material.dart"
    var out: Option[Path] = None
    var root: Option[Path] = flutterRoot
    val curated        = scala.collection.mutable.Set[String]()
    val curatedGeneric = scala.collection.mutable.Map[String, Int]()
    val files   = scala.collection.mutable.ListBuffer[(Path, scala.collection.mutable.Set[String])]()

    for
      raw <- Files.readAllLines(cfgPath).asScala
      line = raw.trim
      if line.nonEmpty && !line.startsWith("#")
    do
      val (key, rest) = line.split("\\s+", 2) match
        case Array(k, r) => (k, r.trim)
        case Array(k)    => (k, "")
      key match
        case "package"      => pkg = rest
        case "import"       => imp = rest
        case "out"          => out = Some(cfgPath.getParent.resolve(rest).normalize)
        case "flutter-root" => if root.isEmpty then root = Some(Paths.get(rest))
        case "curated"      => curated ++= rest.split("\\s+")
        case "curated-generic" =>
          // Entries like `State/1` — curated generic facades with arity.
          for entry <- rest.split("\\s+") do entry.split('/') match
            case Array(n, a) => curatedGeneric(n) = a.toInt
            case _ => sys.error(s"facadegen: bad curated-generic entry '$entry' (want Name/arity)")
        case "file" =>
          val p = Paths.get(rest)
          val resolved =
            if p.isAbsolute then p
            else root.map(_.resolve(p)).getOrElse(
              sys.error("facadegen: relative `file` path but no flutter-root / FLUTTER_ROOT set"))
          files += resolved.normalize -> scala.collection.mutable.Set[String]()
        case "keep" =>
          if files.isEmpty then sys.error("facadegen: `keep` before any `file`")
          files.last._2 ++= rest.split("\\s+")
        case other => sys.error(s"facadegen: unknown config key '$other' in $cfgPath")

    Config(
      scalaPackage   = pkg,
      dartImport     = imp,
      out            = out.getOrElse(sys.error("facadegen: config must set `out`")),
      curated        = curated.toSet,
      curatedGeneric = curatedGeneric.toMap,
      files          = files.map((p, k) => (p, k.toSet)).toList
    )

  // ── Ad-hoc mode ──────────────────────────────────────────────────────

  private def runAdHoc(input: Path, outputDir: Path, importArg: Option[String]): Unit =
    val importPath = importArg.getOrElse(s"package:${input.getFileName}")
    Files.createDirectories(outputDir)
    val dartFiles =
      if Files.isDirectory(input) then collectDartFiles(input) else List(input)
    val json = runDartHelper(dartFiles)
    val dump = JsonParser.parse(json)
    for file <- dump.files do
      val keepAll = file.classes.map(_.name).toSet ++ file.enums.map(_.name).toSet
      val scalaSource = FacadeWriter.render(
        dump.copy(files = List(file)),
        FacadeWriter.GenConfig("generated", importPath, Set.empty, Map.empty, Map(file.path -> keepAll))
      )
      val name = Paths.get(file.path).getFileName.toString.replace(".dart", ".scala")
      val outFile = outputDir.resolve(name)
      Files.writeString(outFile, scalaSource, StandardCharsets.UTF_8)
      println(s"sart-facadegen: wrote $outFile")

  private def collectDartFiles(dir: Path): List[Path] =
    val stream = Files.walk(dir)
    try
      stream.iterator().asScala
        .filter(p => p.toString.endsWith(".dart"))
        .toList.sorted
    finally stream.close()

  // ── Dart helper subprocess ───────────────────────────────────────────

  private val dartBinary: String =
    Option(System.getenv("SART_DART"))
      .orElse(Option(System.getenv("PATH")).flatMap(findOnPath("dart", _)))
      .orElse(Option(System.getenv("FLUTTER_ROOT")).map(r => s"$r/bin/dart"))
      .getOrElse("dart")

  private def findOnPath(exe: String, path: String): Option[String] =
    path.split(java.io.File.pathSeparator).iterator
      .map(dir => Paths.get(dir, exe))
      .find(p => Files.isExecutable(p))
      .map(_.toString)

  private def runDartHelper(dartFiles: List[Path]): String =
    val stdout = new StringBuilder
    val stderr = new StringBuilder
    val logger = ProcessLogger(
      l => { stdout.append(l); stdout.append('\n') },
      l => { stderr.append(l); stderr.append('\n') }
    )
    val cmd = Seq(
      dartBinary, "run",
      "--suppress-analytics",
      toolRoot.resolve("bin/facadegen.dart").toString
    ) ++ dartFiles.map(_.toString)
    val rc = Process(cmd, toolRoot.toFile).!(logger)
    if rc != 0 then
      System.err.println(s"Dart helper failed (rc=$rc):\n${stderr.toString}")
      sys.exit(rc)
    stdout.toString
