package sart.dart

import scala.annotation.StaticAnnotation

// ─── Public API surface (stable for 1.0) ────────────────────────────────
//
// The annotations defined in this file — `@native`, `@DartName`,
// `@DartImport`, `@DartPackage`, `@DartPubspec`, `@DartTopLevel` — and
// the `native.value` sentinel are the stable Sart API. Sart guarantees
// they keep their current names, parameters, and semantics across
// future patch and minor releases.
//
// Everything in `sart.compiler.*` is internal to the emitter and may
// change without notice between minor versions. Integrators (e.g. sbt
// plugins) depend on `sart.compiler.Main`'s CLI contract, not its
// classes directly.
// ────────────────────────────────────────────────────────────────────────

// ─── @native: the core facade marker ───────────────────────────────────────
//
// The class + companion object must live together in the same compilation
// unit (Scala 3 rule). The class is the annotation; the object carries a
// `value: Nothing` sentinel used as the body of facade members:
//
//     def build(context: BuildContext): Widget = dart.native.value
//
// Returning `Nothing` means every member signature stays honest at the type
// level. The runtime body throws, so if a facade is ever invoked on the JVM
// (it shouldn't be — the Sart compiler translates it to Dart first) the
// error is loud rather than silent.

final class native extends StaticAnnotation

object native:
  /** The sentinel value. `Nothing`-typed so it satisfies any member's return
   *  type without further ceremony. Users write it as `dart.native.value`.
   */
  def value: Nothing =
    throw new Error(
      "sart.dart.native.value was evaluated at runtime — the enclosing facade " +
        "was not translated to Dart"
    )

// ─── Naming & import metadata ──────────────────────────────────────────────

/** Overrides the Dart-side name of the annotated symbol. Analogous to
 *  `@scala.scalajs.js.annotation.JSName`.
 */
final class DartName(val name: String) extends StaticAnnotation

/** Declares the Dart import line needed to resolve this facade. The Sart
 *  compiler takes the union of every reachable [[DartImport]] and emits
 *  them at the top of the generated `.dart` file.
 */
final class DartImport(val path: String) extends StaticAnnotation

/** Paired with [[DartImport]]: emits `import '<path>' as <alias>;` and
 *  prefixes every reference to the annotated facade with `<alias>.` —
 *  for libraries whose exports collide with Flutter's (e.g. package:web's
 *  DOM `Text`). A separate annotation (not a default param on
 *  [[DartImport]]) because TASTy keeps annotation applications at source
 *  arity, so adding parameters breaks previously-compiled facades.
 */
final class DartAlias(val alias: String) extends StaticAnnotation

/** Declares a pubspec dependency for the generated project. The Sart
 *  compiler unions every reachable [[DartPackage]] across the compiled
 *  code and writes them into `pubspec.yaml` under `dependencies:`.
 *
 *  - `version`: pub version constraint (e.g. "^1.0.0"). Empty → `any`.
 *  - `sdk`: when non-empty (e.g. "flutter"), emits `<name>: { sdk: <sdk> }`
 *    and `version` is ignored.
 */
final class DartPackage(
  val name: String,
  val version: String = "",
  val sdk: String = ""
) extends StaticAnnotation

/** Injects a verbatim YAML block into the generated `pubspec.yaml`, at the
 *  top level (after `dependencies:`). The Sart compiler collects every
 *  reachable [[DartPubspec]] annotation and writes each unique block in
 *  source order. Use for pubspec keys that aren't dependencies — e.g.
 *  Flutter's `flutter: uses-material-design: true` section, asset
 *  manifests, build-flag stanzas.
 */
final class DartPubspec(val yaml: String) extends StaticAnnotation

/** Marks a Scala facade `object` whose methods correspond to Dart
 *  **top-level** functions rather than methods on a class. The emitter
 *  drops the Scala-side object qualifier at call sites, so
 *  `math.sqrt(x)` in Scala becomes `sqrt(x)` in Dart (matching how
 *  `dart:math` actually exports its API).
 *
 *  Typical use:
 *  {{{
 *  @native @DartImport("dart:math") @DartTopLevel
 *  object math:
 *    def sqrt(x: Double): Double = native.value
 *    val pi: Double = native.value
 *  }}}
 */
final class DartTopLevel extends StaticAnnotation

// ─── JSON codec synthesis ──────────────────────────────────────────────────

/** Marks a case class — or a sealed trait of case classes — for JSON
 *  codec synthesis. Sart emits `fromJson` / `toJson` on the Dart class,
 *  matching the wire shape of json_serializable-generated models:
 *  string-keyed maps, nested `toJson` expansion, and (for members of a
 *  sealed hierarchy) a `type` discriminator with a factory dispatch on
 *  the sealed parent.
 */
final class JsonModel extends StaticAnnotation

/** Overrides the `type` discriminator a sealed-hierarchy member writes
 *  and matches in JSON. Defaults to the class's simple name.
 */
final class JsonTag(val tag: String) extends StaticAnnotation

/** Overrides the JSON key a `@JsonModel` field reads/writes — the
 *  counterpart of json_serializable's `@JsonKey(name: '...')`
 *  (e.g. LightDB's `_id`).
 */
final class JsonField(val name: String) extends StaticAnnotation

/** Dart's non-null assertion. `nn(expr)` emits `expr!` — for facade APIs
 *  that are nullable on the Dart side (e.g. `GlobalKey.currentState`),
 *  where the Scala facade surfaces the value type directly.
 */
object nn:
  def apply[T](value: T): T = value

/** Dart's `as` cast: `cast[RenderBox](x)` emits `(x as RenderBox)`;
 *  `cast[Option[RenderBox]](x)` emits `(x as RenderBox?)`.
 */
object cast:
  def apply[T](value: Any): T = value.asInstanceOf[T]

/** A typed `null` literal — for facade params whose Dart type is nullable
 *  but whose Scala facade type is a primitive Scala can't null
 *  (`TextField(maxLines = nullValue, expands = true)`). Emits `null`.
 */
def nullValue[T]: T = throw new Error("sart.dart.nullValue evaluated at runtime — Sart apps only run as Dart")

/** Dart `await`. Usable anywhere in a method or lambda body — the
 *  enclosing emitted function becomes `async`. Compile-only (a Sart app
 *  never runs on the JVM), so the "synchronous" return type is safe.
 */
object await:
  def apply[T](future: scala.concurrent.Future[T]): T =
    throw new Error("sart.dart.await evaluated at runtime — Sart apps only run as Dart")

/** Marks a whole method body as `async` when the method's declared type
 *  is `Future[T]` but the body is written in direct style:
 *  `def load(): Future[Int] = async { await(fetch()) + 1 }`.
 */
object async:
  def apply[T](body: T): scala.concurrent.Future[T] =
    scala.concurrent.Future.successful(body)

/** Future helpers with direct Dart lowerings. */
object Futures:
  /** `Futures.ensure(f, action)` → Dart `f.whenComplete(action)` — the
   *  `try/finally` of futures: `action` runs on success AND failure, and
   *  the original value/error propagates.
   */
  def ensure[T](f: scala.concurrent.Future[T], action: () => Unit): scala.concurrent.Future[T] = f

  /** `Futures.onError(f, handler)` → Dart `f.catchError(handler)` — the
   *  `catch` of futures.
   */
  def onError[T](f: scala.concurrent.Future[T], handler: Any => T): scala.concurrent.Future[T] = f

  /** `Futures.microtask(body)` → Dart `Future.microtask(body)`. */
  def microtask(body: () => Unit): scala.concurrent.Future[Unit] =
    scala.concurrent.Future.successful(body())

  /** `Futures.delayed(d, body)` → Dart `Future.delayed(d, body)`.
   *  `duration` is `Any` only because sart-dart can't depend on
   *  sart-stdlib's Duration facade — pass a `sart.stdlib.Duration`.
   */
  def delayed(duration: Any, body: () => Unit): scala.concurrent.Future[Unit] =
    scala.concurrent.Future.successful(body())
