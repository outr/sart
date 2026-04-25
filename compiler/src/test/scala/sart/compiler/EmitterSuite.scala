package sart.compiler

import munit.FunSuite
import scala.tasty.inspector.TastyInspector
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*

/** Per-construct unit tests for `DartEmitter`.
 *
 *  Strategy: a small set of fixture classes under `fixtures/` is compiled
 *  to TASTy by sbt (`Test / scalacOptions += "-Yretain-trees"`), the
 *  emitter runs against the test classes directory once per JVM, and each
 *  test asserts on substrings of the resulting `lib/main.dart`. Cheaper
 *  than the all-or-nothing golden-file diff and pinpoints which Scala
 *  construct regressed.
 */
class EmitterSuite extends FunSuite:

  // Lazy + shared so the inspection cost (a few seconds) is paid once.
  private lazy val emittedMain: String = {
    val tastyRoot = locateTestClassesDir(classOf[fixtures.FxPoint])
    val outDir    = Files.createTempDirectory("sart-emitter-test")
    val tastyFiles = findTastyFiles(tastyRoot)
    // Pare the inspector classpath down to the stdlib jars + the
    // fixtures' own classes dir. The full test classpath drags in
    // scala3-compiler (transitively via munit), which exposes its own
    // Predef and breaks symbol resolution against the fixtures' TASTy.
    val cp = sys.props.getOrElse("java.class.path", "")
      .split(java.io.File.pathSeparator).toList
      .filter(_.nonEmpty)
      .filter { p =>
        p.endsWith("test-classes") ||
        p.contains("scala-library")  ||
        p.contains("scala3-library")
      }
    val emitter = new DartEmitter(outDir)
    val ok = TastyInspector.inspectAllTastyFiles(tastyFiles, Nil, cp)(emitter)
    require(ok, "TASTy inspection failed")
    emitter.writeOutput()
    new String(Files.readAllBytes(outDir.resolve("lib/main.dart")))
  }

  private def locateTestClassesDir(cls: Class[?]): Path =
    Paths.get(cls.getProtectionDomain.getCodeSource.getLocation.toURI)

  private def findTastyFiles(root: Path): List[String] =
    // Only inspect the fixtures package — the suite itself ends up in
    // the same classes directory, and pointing the inspector at e.g.
    // EmitterSuite.tasty fails because munit isn't on the inspector
    // classpath.
    val s = Files.walk(root)
    val sep = java.io.File.separator
    val needle = s"sart${sep}compiler${sep}fixtures${sep}"
    try s.iterator().asScala
      .filter(_.toString.endsWith(".tasty"))
      .filter(_.toString.contains(needle))
      .map(_.toString).toList.sorted
    finally s.close()

  /** Slice the emitted Dart down to the body of one class so a substring
   *  match doesn't accidentally hit a sibling fixture's emission. */
  private def classBody(name: String): String =
    val start = emittedMain.indexOf(s"class $name")
    require(start >= 0, s"class $name not found in emitted output")
    val tail  = emittedMain.substring(start)
    // crude but good enough: stop at the next top-level `class ` or EOF
    val end = tail.indexOf("\nclass ", 1) match
      case -1 => tail.length
      case n  => n
    tail.substring(0, end)

  // ── Tests ────────────────────────────────────────────────────────────

  test("case class emits ==/hashCode/toString/copyWith") {
    val body = classBody("FxPoint")
    assert(body.contains("bool operator =="),  body)
    assert(body.contains("int get hashCode"),  body)
    assert(body.contains("String toString()"), body)
    assert(body.contains("copyWith("),         body)
    // Field declarations are immutable.
    assert(body.contains("final int x"),       body)
    assert(body.contains("final int y"),       body)
  }

  test("plain class emits a method without value-equality boilerplate") {
    val body = classBody("FxCounter")
    assert(body.contains("int increment(int n)"), body)
    assert(!body.contains("operator =="),         body)
    assert(!body.contains("copyWith"),            body)
  }

  test("sealed trait becomes sealed Dart class with subclass extends") {
    assert(emittedMain.contains("sealed class FxShape"),       emittedMain)
    assert(emittedMain.contains("class FxCircle extends FxShape"), emittedMain)
    assert(emittedMain.contains("class FxSquare extends FxShape"), emittedMain)
  }

  test("pattern match on Int compiles to a Dart switch expression") {
    val body = classBody("FxPatternMatch")
    assert(body.contains("switch (n)"), body)
    assert(body.contains("0 => 'zero'"), body)
    assert(body.contains("1 => 'one'"),  body)
    assert(body.contains("_ => 'other'"), body)
  }

  test("generic class carries type parameters through to Dart") {
    val body = classBody("FxGeneric")
    assert(body.contains("class FxGeneric<T>"), body)
    assert(body.contains("final T value"),      body)
    assert(body.contains("T unwrap()"),         body)
  }

  test("extension method emits as a Dart extension on the receiver type") {
    assert(emittedMain.contains("extension"),    emittedMain)
    assert(emittedMain.contains("on String"),    emittedMain)
    assert(emittedMain.contains("fxShout"),      emittedMain)
    assert(emittedMain.contains("toUpperCase()"), emittedMain)
  }

  test("string + Int auto-coerces the Int via .toString()") {
    val body = classBody("FxStringConcat")
    // Either form is acceptable — what matters is that the Int isn't
    // passed raw to Dart's `+`, which would be a type error.
    assert(
      body.contains("n.toString()") || body.contains("'$n'"),
      body
    )
  }

  test("for-comprehension over List becomes .map(...).toList") {
    val body = classBody("FxForComp")
    assert(body.contains(".map("),  body)
    assert(body.contains("x * x"),  body)
    assert(body.contains(".toList"), body)
  }

  test("List.take/drop/takeWhile/dropWhile map to Dart take/skip/takeWhile/skipWhile") {
    val body = classBody("FxListOps")
    assert(body.contains("xs.take(3)"),         body)
    assert(body.contains("xs.skip(2)"),         body)
    assert(body.contains("xs.takeWhile("),      body)
    assert(body.contains("xs.skipWhile("),      body)
  }

  test("List.exists/forall map to Dart any/every") {
    val body = classBody("FxListOps")
    assert(body.contains("xs.any("),  body)
    assert(body.contains("xs.every("), body)
  }

  test("List.contains and indexOf pass through to Dart same-named ops") {
    val body = classBody("FxListOps")
    assert(body.contains("xs.contains(0)"), body)
    assert(body.contains("xs.indexOf(0)"),  body)
  }

  test("List.init and List.tail polyfill via sublist") {
    val body = classBody("FxListOps")
    assert(body.contains("xs.sublist(0, xs.length - 1)"), body)
    assert(body.contains("xs.sublist(1)"),                body)
  }

  test("Map.get / getOrElse use Dart's index syntax with `??`") {
    val body = classBody("FxMapOps")
    assert(body.contains("m[k]"),         body)
    assert(body.contains("(m[k] ?? 0)"),  body)
  }

  test("Map getters: size→length, nonEmpty→isNotEmpty, keys/values pass through") {
    val body = classBody("FxMapOps")
    assert(body.contains("m.length"),     body)
    assert(body.contains("m.isNotEmpty"), body)
    assert(body.contains("m.keys"),       body)
    assert(body.contains("m.values"),     body)
  }

  test("Option.orElse / contains map to nullable polyfills") {
    val body = classBody("FxOptionOps")
    assert(body.contains("(o ?? d)"),     body)
    assert(body.contains("(o == x)"),     body)
  }

  test("Option.exists / forall guard the call by null") {
    val body = classBody("FxOptionOps")
    // exists: o != null && p(o!)
    assert(body.contains("o != null") && body.contains("o!"), body)
    // forall: o == null || p(o!)
    assert(body.contains("o == null"), body)
  }

  test("Option.filter returns the value-or-null based on the predicate") {
    val body = classBody("FxOptionOps")
    // (o != null && p(o!) ? o : null)
    assert(body.contains("? o : null"), body)
  }

  test("Set.size→length and Set.nonEmpty→isNotEmpty (carved out of list-like)") {
    val body = classBody("FxSetOps")
    assert(body.contains("s.length"),     body)
    assert(body.contains("s.isNotEmpty"), body)
  }

  test("List.find returns nullable via inline materialise+pick") {
    val body = classBody("FxIterableOps")
    // Polyfill: materialise the matches, return null if empty, else first.
    assert(body.contains(".where(") && body.contains("isEmpty ? null"), body)
  }

  test("List.count maps to .where(p).length") {
    val body = classBody("FxIterableOps")
    assert(body.contains("xs.where(") && body.contains(".length"), body)
  }

  test("String.toInt / toDouble call Dart's static parse") {
    val body = classBody("FxStringOps")
    assert(body.contains("int.parse(s)"),    body)
    assert(body.contains("double.parse(s)"), body)
  }

  test("String.stripMargin polyfills via a multiline RegExp replaceAll") {
    val body = classBody("FxStringOps")
    assert(body.contains("RegExp(") && body.contains("multiLine: true"), body)
  }

  test("List.flatten polyfills via expand((x) => x)") {
    val body = classBody("FxListStructural")
    assert(body.contains("xss.expand((x) => x).toList()"), body)
  }

  test("List.distinct goes through Set to dedupe") {
    val body = classBody("FxListStructural")
    assert(body.contains("xs.toSet().toList()"), body)
  }

  test("List.sorted uses Dart's cascade-mutate-and-return idiom") {
    val body = classBody("FxListStructural")
    assert(body.contains("..sort()"), body)
  }

  test("List.sortBy passes a key extractor through Comparable.compare") {
    val body = classBody("FxListStructural")
    assert(body.contains("Comparable.compare"), body)
    assert(body.contains("..sort("),            body)
  }

  test("List.foldRight reverses and swaps lambda arg order") {
    val body = classBody("FxListStructural")
    assert(body.contains("xs.reversed.fold("), body)
    // Args swapped: Scala op(a, acc) → Dart fold passes (acc, a),
    // so the wrapping lambda calls userOp(a, acc).
    assert(body.contains("(a, acc)"), body)
  }

  test("strict-mode invariant: no /* TODO */ markers in fixture emission") {
    // Catches regressions where a previously-handled tree shape starts
    // falling through to the unhandled-case branch.
    val todos = emittedMain.linesIterator
      .filter(_.contains("/* TODO"))
      .toList
    assert(todos.isEmpty, todos.mkString("\n"))
  }
