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
  private lazy val emittedMain: String = emitted._2
  private lazy val emitter: DartEmitter = emitted._1
  private lazy val emitted: (DartEmitter, String) = {
    // The build injects the real test-classes directory (sbt 2 presents
    // classpaths as hashed cache jars, so code-source lookup lands on a
    // jar without the fixtures' .tasty files). The code-source fallback
    // keeps the suite runnable from IDEs.
    val tastyRoot = sys.props.get("sart.test.classesDir")
      .map(Paths.get(_))
      .getOrElse(locateTestClassesDir(classOf[fixtures.FxPoint]))
    require(Files.isDirectory(tastyRoot), s"test classes dir not found: $tastyRoot")
    emit(tastyRoot, gaps = false)
  }

  /** A second, separate emission over `fixtures.gaps` — constructs the
   *  emitter deliberately does NOT translate, kept out of the main
   *  fixture set so its strict-mode invariant stays meaningful. */
  private lazy val gapEmitter: DartEmitter = {
    val tastyRoot = sys.props.get("sart.test.classesDir")
      .map(Paths.get(_))
      .getOrElse(locateTestClassesDir(classOf[fixtures.FxPoint]))
    emit(tastyRoot, gaps = true)._1
  }

  private def emit(tastyRoot: Path, gaps: Boolean): (DartEmitter, String) = {
    val outDir    = Files.createTempDirectory("sart-emitter-test")
    val tastyFiles = findTastyFiles(tastyRoot, gaps)
    require(tastyFiles.nonEmpty, s"no fixture .tasty files under $tastyRoot")
    // Pare the inspector classpath down to the stdlib jars + the
    // fixtures' own classes dir. The full test classpath drags in
    // scala3-compiler (transitively via munit), which exposes its own
    // Predef and breaks symbol resolution against the fixtures' TASTy.
    // Stdlib jars are located via code source so jar naming is irrelevant.
    val cp = List(
      tastyRoot.toString,
      codeSourcePath(classOf[scala.runtime.TupleXXL]), // scala3-library
      codeSourcePath(classOf[scala.Option[?]]),        // scala-library
      codeSourcePath(classOf[sart.dart.native])        // sart-dart annotations
    ).distinct
    val emitter = new DartEmitter(outDir, wireMappings = Map(
      "fxforeign.FxId"      -> "String",
      "fxforeign.FxStamp"   -> "int",
      "fxforeign.FxWrapped" -> "FxWrapTarget",
      "fxforeign.FxDir"     -> "String"
    ))
    val ok = TastyInspector.inspectAllTastyFiles(tastyFiles, Nil, cp)(emitter)
    require(ok, "TASTy inspection failed")
    emitter.writeOutput()
    (emitter, new String(Files.readAllBytes(outDir.resolve("lib/main.dart"))))
  }

  private def locateTestClassesDir(cls: Class[?]): Path =
    Paths.get(cls.getProtectionDomain.getCodeSource.getLocation.toURI)

  private def codeSourcePath(cls: Class[?]): String =
    Paths.get(cls.getProtectionDomain.getCodeSource.getLocation.toURI).toString

  private def findTastyFiles(root: Path, gaps: Boolean): List[String] =
    // Only inspect the fixtures package — the suite itself ends up in
    // the same classes directory, and pointing the inspector at e.g.
    // EmitterSuite.tasty fails because munit isn't on the inspector
    // classpath.
    val s = Files.walk(root)
    val sep = java.io.File.separator
    val needle = s"sart${sep}compiler${sep}fixtures${sep}"
    val gapsNeedle = s"${needle}gaps${sep}"
    try s.iterator().asScala
      .filter(_.toString.endsWith(".tasty"))
      .filter(_.toString.contains(needle))
      .filter(_.toString.contains(gapsNeedle) == gaps)
      .map(_.toString).toList.sorted
    finally s.close()

  /** Slice the emitted Dart down to the body of one class so a substring
   *  match doesn't accidentally hit a sibling fixture's emission. */
  private def classBody(name: String, kind: String = "class"): String =
    val start = emittedMain.indexOf(s"$kind $name")
    require(start >= 0, s"$kind $name not found in emitted output")
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
    assert(body.contains("(o ?? (d))"),   body)
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

  test("Tuple type → Dart record type with parens") {
    val body = classBody("FxTuples")
    // Method signatures: `(int, String) pair(...)`, `(int, String, int) triple(...)`.
    assert(body.contains("(int, String) pair(") || body.contains("(int, String) Function"), body)
    assert(body.contains("(int, String, int) triple"), body)
  }

  test("Tuple literal → Dart record literal with same syntax") {
    val body = classBody("FxTuples")
    assert(body.contains("(a, b)"),    body)
    assert(body.contains("(a, b, c)"), body)
  }

  test("Tuple field access ._N → Dart positional getter .$N") {
    val body = classBody("FxTuples")
    assert(body.contains("t.$1"), body)
    assert(body.contains("t.$2"), body)
    assert(body.contains("t.$3"), body)
  }

  test("List.zip pairs by index up to the shorter length") {
    val body = classBody("FxPairing")
    assert(body.contains("List.generate"),  body)
    assert(body.contains("(xs[i], ys[i])"), body)
  }

  test("List.zipWithIndex builds (elem, index) pairs in Scala order") {
    val body = classBody("FxPairing")
    // Scala puts the index second in each pair.
    assert(body.contains("(xs[i], i)"), body)
  }

  test("List.partition returns a tuple of (matching, not-matching)") {
    val body = classBody("FxPairing")
    assert(body.contains(".where(") && body.contains("!("), body)
    assert(body.contains(".toList(),"),                     body)
  }

  test("Map.updated emits as a Dart spread-and-add map literal") {
    val body = classBody("FxMapMutators")
    assert(body.contains("{...m, k: v}"), body)
  }

  test("List.sum / product fold from a literal zero/one") {
    val body = classBody("FxNumericOps")
    assert(body.contains("xs.fold(0, (a, b) => a + b)"), body)
    assert(body.contains("xs.fold(1, (a, b) => a * b)"), body)
  }

  test("List.min / max use reduce with a comparison ternary") {
    val body = classBody("FxNumericOps")
    assert(body.contains("xs.reduce((a, b) => a < b ? a : b)"), body)
    assert(body.contains("xs.reduce((a, b) => a > b ? a : b)"), body)
  }

  test("Range `1 to n` materialises as List<int>.generate inclusive") {
    val body = classBody("FxRanges")
    assert(body.contains("List<int>.generate(n - 1 + 1"), body)
  }

  test("Range `0 until n` materialises as List<int>.generate exclusive") {
    val body = classBody("FxRanges")
    assert(body.contains("List<int>.generate(n - 0,"), body)
  }

  test("Range `.map(f)` chains through the list-like rewrite to .map().toList()") {
    val body = classBody("FxRanges")
    assert(body.contains(".map(") && body.contains(".toList()"), body)
  }

  test("user object emits as a Dart class of static members") {
    val body = classBody("FxRegistry")
    assert(body.contains("FxRegistry._();"), body)
    assert(body.contains("static String current"), body)
    assert(body.contains("static final int limit = 10;"), body)
    assert(body.contains("static bool get isActive {"), body)
    assert(body.contains("static String register(String name) {"), body)
    assert(!emittedMain.contains("FxRegistry$"), "module val or class leaked")
  }

  test("@JsonModel case class gets fromJson/toJson in json_serializable shape") {
    val body = classBody("FxUser")
    assert(body.contains("static FxUser fromJson(Map<String, dynamic> json) =>"), body)
    assert(body.contains("(json['name'] as String)"), body)
    assert(body.contains("(json['age'] as num).toInt()"), body)
    assert(body.contains("(json['tags'] as List<dynamic>).map((e) => (e as String)).toList()"), body)
    assert(body.contains("json['nickname'] == null ? null : (json['nickname'] as String)"), body)
    assert(body.contains("Map<String, dynamic> toJson() => {"), body)
    assert(body.contains("'name': name,"), body)
  }

  test("@DartImport alias prefixes references and the import line") {
    assert(emittedMain.contains("import 'dart:math' as fxmath;"), emittedMain.linesIterator.take(8).mkString("\n"))
    val body = classBody("FxAliasUse")
    assert(body.contains("fxmath.sqrt(x)"), body)
  }

  test("super calls and List.foreach emit") {
    val body = classBody("FxLifecycle")
    assert(body.contains("super.initState();"), body)
    val fe = classBody("FxForeach")
    assert(fe.contains("xs.forEach(f);"), fe)
    assert(fe.contains("[...(xs), ...(ys)]"), fe)
  }

  test("scala.Some / scala.None lower like the sart.stdlib pair") {
    val body = classBody("FxScalaOpt")
    assert(body.contains("id == 0 ? null : 'found'"), body)
  }

  test("@JsonField overrides the JSON key") {
    val body = classBody("FxKeyed")
    assert(body.contains("(json['_id'] as String)"), body)
    assert(body.contains("'_id': id,"), body)
  }

  test("Json.decode/encode lower to the synthesized codecs") {
    val body = classBody("FxJsonBridge")
    assert(body.contains("FxUser.fromJson(d as Map<String, dynamic>)"), body)
    assert(body.contains("u.toJson()"), body)
  }

  test("field-less sealed members still get tagged codecs") {
    val basic = classBody("FxKindBasic")
    assert(basic.contains("static FxKindBasic fromJson(Map<String, dynamic> json) =>"), basic)
    assert(basic.contains("'type': 'Kind.Basic',"), basic)
  }

  test("@JsonModel sealed hierarchy dispatches on the type tag") {
    val parent = classBody("FxWire")
    assert(parent.contains("static FxWire fromJson(Map<String, dynamic> json) {"), parent)
    assert(parent.contains("if (t == 'Wire.Ping') return FxPing.fromJson(json);"), parent)
    assert(parent.contains("if (t == 'FxPong') return FxPong.fromJson(json);"), parent)
    assert(parent.contains("Map<String, dynamic> toJson();"), parent)
    val ping = classBody("FxPing")
    assert(ping.contains("'type': 'Wire.Ping',"), ping)
  }

  test("subclassing a user class emits a super(...) initializer") {
    val body = classBody("FxDerived")
    assert(body.contains("FxDerived(String label, this.extra) : super(label);"), body)
    assert(!body.contains("final String label;"), body) // non-val param is not a field
  }

  test("trait in second parent position emits as a with-clause mixin") {
    val mixin = classBody("FxClickable")
    assert(emittedMain.contains("mixin class FxClickable"), mixin)
    val cls = classBody("FxButtonish")
    assert(cls.contains("extends FxBase with FxClickable"), cls)
    assert(cls.contains(": super('btn');"), cls)
  }

  test("literal default params emit as a Dart named section") {
    val body = classBody("FxNamedParams")
    assert(body.contains("String greet(String name, {String punct = '!', int times = 1})"), body)
  }

  test("call sites match the emitted named/positional shape") {
    val body = classBody("FxNamedParams")
    assert(body.contains("greet('a')"), body)          // omitted defaults stripped
    assert(body.contains("greet('a', times: 3)"), body) // named stays named
    assert(body.contains("greet('a', punct: '.')"), body) // positional-at-default becomes named
  }

  test("case-class ctor defaults emit as named ctor params; copyWith matches") {
    val body = classBody("FxStyled")
    assert(body.contains("FxStyled(this.label, {this.size = 12, this.bold = false});"), body)
    assert(body.contains("size: size ?? this.size"), body)
    val calls = classBody("FxNamedCtor")
    assert(calls.contains("FxStyled('x')"), calls)
    assert(calls.contains("FxStyled('x', bold: true)"), calls)
    assert(calls.contains("copyWith(size: 20)"), calls)
  }

  test("Future.successful / Future.failed emit Dart Future.value / Future.error") {
    val body = classBody("FxFutureCtor")
    assert(body.contains("Future.value(n)"), body)
    assert(body.contains("Future.error(e)"), body)
  }

  test("while loop emits a Dart while statement") {
    val body = classBody("FxWhileLoop")
    assert(body.contains("while (i > 0) {"), body)
    assert(body.contains("total = total + i;"), body)
  }

  test("lazy val field emits as late final") {
    val body = classBody("FxLazyInit")
    assert(body.contains("late final int expensive = 6 * 7;"), body)
    // Local lazy vals are desugared (and here inlined) by scalac before
    // the emitter sees them — assert the method still emits cleanly.
    assert(body.contains("int local(int n)"), body)
  }

  test("strict-mode invariant: no /* TODO */ markers in fixture emission") {
    // Catches regressions where a previously-handled tree shape starts
    // falling through to the unhandled-case branch.
    val todos = emittedMain.linesIterator
      .filter(_.contains("/* TODO"))
      .toList
    assert(todos.isEmpty, todos.mkString("\n"))
  }

  test("await/async: direct-style bodies emit Dart async functions") {
    val body = classBody("FxAsync")
    assert(body.contains("Future<int> load() async {"), body)
    assert(body.contains("(await fetch())"), body)
    assert(body.contains("void fire() async {"), body)
  }

  test("Option match lowers to a null-check on a promoted local") {
    val body = classBody("FxOptionMatch")
    assert(body.contains("final v = o;"), body)
    assert(body.contains("if (v != null)"), body)
    assert(body.contains("return 'none';"), body)
  }

  test("extension-method call sites emit as receiver.method(args)") {
    val body = classBody("FxExtUse")
    assert(body.contains("s.fxRepeat(2)"), body)
  }

  test("parented parameterless trait emits as mixin-on") {
    assert(emittedMain.contains("mixin FxMixinOn on FxCounter {"), emittedMain)
    assert(emittedMain.contains("class FxWithMixinOn extends FxCounter with FxMixinOn"), emittedMain)
  }

  test("plain case classes get codecs without any annotation") {
    val body = classBody("FxPlainModel")
    assert(body.contains("static FxPlainModel fromJson(Map<String, dynamic> json) =>"), body)
    assert(body.contains("(json['id'] as String)"), body)
    assert(body.contains("Map<String, dynamic> toJson() => {"), body)
    assert(body.contains("'note': note,"), body)
  }

  test("case classes with non-wire fields get no codec") {
    val body = classBody("FxNotWire")
    assert(!body.contains("fromJson"), body)
    assert(!body.contains("toJson"), body)
  }

  test("classes nested in objects flatten, and sealed members default to fabric-style tags") {
    val circle = classBody("FxGeomCircle")
    assert(circle.contains("class FxGeomCircle extends FxGeom"), circle)
    assert(circle.contains("'type': 'FxGeom.Circle',"), circle)
    val parent = classBody("FxGeom")
    assert(parent.contains("if (t == 'FxGeom.Box') return FxGeomBox.fromJson(json);"), parent)
    val use = classBody("FxGeomUse")
    assert(use.contains("FxGeomCircle(r)"), use)
    assert(use.contains("FxGeom.fromJson(d as Map<String, dynamic>)"), use)
  }

  test("JVM-only givens in companions are skipped loudly") {
    val parent = classBody("FxGeom")
    assert(!parent.contains("randomUUID"), parent)
    assert(parent.contains("`rw: fabric.rw.RW` is JVM-only"), parent)
  }

  test("fabric.Json rides as dynamic and its builders lower to literals") {
    val rec = classBody("FxApiRecord")
    assert(rec.contains("final dynamic extra;"), rec)
    assert(rec.contains("final List<dynamic> tags;"), rec)
    assert(rec.contains("static FxApiRecord fromJson"), rec)
    assert(rec.contains("'extra': extra,"), rec)
    val c = classBody("FxApiConsts")
    assert(c.contains("{'type': 'SourceType.Private'}"), c)
    assert(c.contains("<String, dynamic>{}"), c)
    assert(c.contains("[1, true]"), c)
  }


  test("sealed-trait-of-case-objects emits a Dart enum with fabric-style string codecs") {
    val start = emittedMain.indexOf("enum FxColor {")
    assert(start >= 0, "enum FxColor not emitted")
    val e = emittedMain.substring(start, emittedMain.indexOf("\n}", start) + 2)
    assert(e.contains("Red, Blue;"), e)
    assert(e.contains("FxColor.Red => 'FxColor.Red',"), e)
    assert(e.contains("static FxColor fromJson(dynamic json)"), e)
    assert(e.contains("`rw: fabric.rw.RW` is JVM-only"), e)
    assert(e.contains("`values` is built in on Dart enums"), e)
    assert(e.contains("static FxColor parse(String s)"), e)
    assert(!emittedMain.contains("class FxColorRed"), "enum members must not emit as classes")
  }

  test("enum-typed fields ride on the string codecs") {
    val p = classBody("FxPaint")
    assert(p.contains("FxColor.fromJson(json['color'])"), p)
    assert(p.contains("'color': color.toJson(),"), p)
    assert(p.contains("'alt': alt?.toJson(),"), p)
  }

  test("enum members are referenced as qualified constants in values and patterns") {
    val u = classBody("FxColorUse")
    assert(u.contains("c == FxColor.Red"), u)
    assert(u.contains("FxColor.Red => 'red'"), u)
    assert(u.contains("return FxColor.Blue;"), u)
  }

  test("case objects in mixed hierarchies are const singletons with tagged codecs") {
    val eof = classBody("FxTokenEOF")
    assert(eof.contains("class FxTokenEOF extends FxToken {"), eof)
    assert(eof.contains("const FxTokenEOF();"), eof)
    assert(eof.contains("'type': 'FxToken.EOF',"), eof)
    val parent = classBody("FxToken")
    assert(parent.contains("if (t == 'FxToken.EOF') return FxTokenEOF.fromJson(json);"), parent)
    val use = classBody("FxTokenUse")
    assert(use.contains("return const FxTokenEOF();"), use)
    assert(use.contains("const FxTokenEOF() => true"), use)

  }

  test("types Dart cannot express erase to dynamic; match types reduce") {
    val g = classBody("FxGraph")
    assert(g.contains("List<dynamic> get nodes;"), g)
    assert(g.contains("dynamic get first {"), g)
    val ig = classBody("FxIntGraph")
    assert(ig.contains("List<int> get nodes {"), ig)
    val t = classBody("FxTypes")
    assert(t.contains("static dynamic firstOf(FxGraph g)"), t)
    assert(t.contains("static String either(dynamic x)"), t)
    assert(t.contains("static int both(dynamic x)"), t)
    assert(t.contains("static int refined(FxGraph x)"), t)
    assert(t.contains("static bool same(FxGraph g, FxGraph h)"), t)
    assert(t.contains("static String show(dynamic u)"), t)
    assert(t.contains("static dynamic lift<F, A>(FxTypesFunctor<F> fn, dynamic fa, A Function(A) f)"), t)
    assert(t.contains("static int head(List<int> xs)"), t)
    val uid = classBody("FxTypesUserId")
    assert(uid.contains("static dynamic apply(String s)"), uid)
    assert(!emittedMain.contains("<none>"), "unrepresentable type leaked as <none>")
  }

  test("nested givens keep their $ class, declare the abstraction, and narrow erased params covariantly") {
    val t = classBody("FxTypes")
    assert(t.contains("static late final FxTypesFunctor<FxBox> boxFunctor = FxTypesboxFunctor$();"), t)
    assert(!t.contains("FxTypesUserId UserId"), "plain nested object must not emit a pointer val: " + t)
    val inst = classBody("FxTypesboxFunctor$")
    assert(inst.contains("FxBox<B> fmap<A, B>(covariant FxBox<A> fa, B Function(A) f)"), inst)
  }

  test("sealed parents of case objects declare a const constructor") {
    val parent = classBody("FxToken")
    assert(parent.contains("const FxToken();"), parent)
  }

  test("a case object without a const-able parent chain becomes a singleton factory") {
    val solo = classBody("FxSolo")
    assert(solo.contains("factory FxSolo() => _instance;"), solo)
    assert(solo.contains("static final FxSolo _instance = FxSolo._();"), solo)
    assert(solo.contains("FxSolo._() : super(1);"), solo)
    assert(!solo.contains("const FxSolo"), solo)
    assert(!classBody("FxSoloBase").contains("const FxSoloBase"), classBody("FxSoloBase"))
    assert(classBody("FxSoloUse").contains("return FxSolo();"), classBody("FxSoloUse"))
  }

  test("unsupported constructs are collected with their Scala source location") {
    val gaps = gapEmitter.unsupported.toList
    assert(gaps.exists(_.matches(".*Gaps\\.scala:\\d+: nested def")), gaps.mkString("\n"))
    assert(emitter.unsupported.isEmpty, emitter.unsupported.mkString("\n"))
  }


  test("class-body statements become the Dart constructor body") {
    val b = classBody("FxCtorBody")
    assert(b.contains("FxCtorBody() {\n    bump();\n  }"), b)
    val p = classBody("FxCtorBodyParams")
    assert(p.contains("FxCtorBodyParams(this.a) {\n    total = a * 2;\n  }"), p)
  }


  test("Option(x) and Option.empty lower to the nullable value") {
    val b = classBody("FxOptionApply")
    assert(b.contains("String? wrap(String s) {\n    return s;\n  }"), b)
    assert(b.contains("int? none() {\n    return null;\n  }"), b)
  }


  test("@DartLibrary routes emission into per-platform libraries with their own headers") {
    val libs = emitter.renderedLibraries
    val stub = libs("platform/fx_fs_stub.dart")
    assert(stub.startsWith("import '../main.dart';"), stub.linesIterator.take(3).mkString("\n"))
    assert(stub.contains("import '../sart_option.dart';"), stub)
    assert(stub.contains("class FxFs {"), stub)
    assert(stub.contains("static bool isFullscreen()"), stub)
    val web = libs("platform/fx_fs_web.dart")
    assert(web.contains("class FxFs {"), web)
    assert(!emittedMain.contains("\nclass FxFs {"), "variant impls must not land in main.dart")
    assert(!emittedMain.contains("FxFsStub"), "variant impls must not land in main.dart")
  }

  test("@DartVariants generates the conditional-export switch; call sites use the facade") {
    val switch = emitter.renderedLibraries("platform/fx_fs.dart")
    assert(switch.contains(
      "export 'fx_fs_stub.dart' if (dart.library.js_interop) 'fx_fs_web.dart';"), switch)
    assert(emittedMain.contains("import 'platform/fx_fs.dart';"), "facade import missing in main")
    assert(classBody("FxFsUse").contains("return FxFs.isFullscreen();"), classBody("FxFsUse"))
  }


  test("@JsonTag on enum members overrides the wire value") {
    val lvl = classBody("FxReviewLevel", kind = "enum")
    assert(lvl.contains("FxReviewLevel.Movie => 'movie',"), lvl)
    assert(lvl.contains("FxReviewLevel.Episode => 'episode',"), lvl)
    assert(lvl.contains("FxReviewLevel.Other => 'FxReviewLevel.Other',"), lvl)
    val q = classBody("FxQuality", kind = "enum")
    assert(q.contains("FxQuality.Low => 'sd',"), q)
    assert(q.contains("FxQuality.High => 'hd',"), q)
  }

  test("enum wire values derive from the companion fabric RW chain (leaf.lowerCase)") {
    val r = classBody("FxRating", kind = "enum")
    assert(r.contains("FxRating.Movie => 'movie',"), r)
    assert(r.contains("// `rw: fabric.rw.RW` is JVM-only (no Dart form) — not emitted") || !r.contains("rw"), r)
    val sp = classBody("FxSpeed", kind = "enum")
    assert(sp.contains("FxSpeed.Slow => 'slow',"), sp)
  }

  test("underscore/keyword Scala field names stay the wire key, sanitised in Dart") {
    val d = classBody("FxDoc")
    assert(d.contains("final String id;"), d)
    assert(d.contains("FxDoc(this.type, this.id, this.value);"), d)
    assert(d.contains("json['_id']"), d)
    assert(d.contains("'_id': id,"), d)
    assert(d.contains("'type': type,"), d)
    assert(classBody("FxDocUse").contains("return d.id;"), classBody("FxDocUse"))
    assert(classBody("FxDocUse").contains("return d.type;"), classBody("FxDocUse"))
  }

  test("fabric @serialized and @typeField from shared code drive the wire keys") {
    val ser = classBody("FxSer")
    assert(ser.contains("json['df']"), ser)
    assert(ser.contains("'df': displayField,"), ser)
    val root = classBody("FxShapeK")
    assert(root.contains("json['kind'] as String"), root)
    val circle = classBody("FxCircleK")
    assert(circle.contains("'kind': 'FxCircleK',"), circle)
  }

  test("Set fields encode as JSON arrays and decode back to sets") {
    val t = classBody("FxTags")
    assert(t.contains("'tags': tags.toList(),"), t)
    assert(t.contains("(json['tags'] as List<dynamic>)"), t)
    assert(t.contains(".toSet()"), t)
    assert(t.contains("geoms.map((e) => e.toJson()).toList(),"), t)
  }

  test("wire-mapped foreign types: primitives in type position, codecs cast/delegate") {
    val r = classBody("FxRow")
    assert(r.contains("final String id;"), r)
    assert(r.contains("final int created;"), r)
    assert(r.contains("final FxWrapTarget who;"), r)
    assert(!r.contains("class FxRow extends"), "foreign parent must be dropped: " + r)
    assert(r.contains("(json['_id'] as String)"), r)
    assert(r.contains("(json['created'] as num).toInt()"), r)
    assert(r.contains("FxWrapTarget.fromJson(json['who'] as Map<String, dynamic>)"), r)
    assert(r.contains("'who': who.toJson(),"), r)
    assert(emittedMain.contains("// `FxRow` companion extends a JVM-only base — not emitted"), "schema companion must be skipped")
    assert(!emittedMain.contains("schemaThing"), "schema companion members must not emit")
    assert(classBody("FxRowUse").contains("return r.id;"), classBody("FxRowUse"))
  }

  test("members with JVM-only signatures are skipped with a comment") {
    val ops = classBody("FxRowOps")
    assert(ops.contains("// `schemaLookup` has a JVM-only signature (no Dart form) — not emitted"), ops)
    assert(!ops.contains("schemaLookup("), ops)
    assert(ops.contains("static int fine()"), ops)
  }

  test("wire-mapped constructions: wrappers are transparent, class targets construct") {
    val m = classBody("FxRowMake")
    assert(m.contains("return FxRow('r1', 5, FxWrapTarget('a', 1));"), m)
  }


  test("case objects of a String-mapped foreign enumeration emit wire literals") {
    val d = classBody("FxDirUse")
    assert(d.contains("return d == 'FxDir.Up';"), d)
  }

  test("toString on an emitted enum is its wire value (toJson)") {
    assert(emittedMain.contains("FxColorLabel"), "fixture present")
    val l = classBody("FxColorLabel")
    assert(l.contains("return c.toJson();"), l)
  }

  test("two-arg RW.enumeration yields bare member wire names") {
    val sig = classBody("FxSignal", kind = "enum")
    assert(sig.contains("FxSignal.Sell => 'Sell',"), sig)
  }

  test("underscore-strip collisions fall back to a trailing underscore") {
    val b = classBody("FxBoth")
    assert(b.contains("final int id;"), b)
    assert(b.contains("final String id_;"), b)
    assert(b.contains("json['_id']"), b)
    assert(classBody("FxBothUse").contains("return b.id_;"), classBody("FxBothUse"))
  }

  test("flatMap over Option-returning lambdas guards nulls; mkString getter and Set forms map to join") {
    val f = classBody("FxFlatOpt")
    assert(f.contains("== null ? const [] : ["), f)
    assert(f.contains(".join()"), f)
    assert(f.contains("s.join(', ')"), f)
  }

  test("flatten: Option elements null-guard, List elements spread") {
    val f = classBody("FxFlattenKinds")
    assert(f.contains("vOpt == null ? const [] : [vOpt]"), f)
    assert(f.contains(".expand((x) => x).toList()"), f)
  }
