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

// ─── Platform-variant emission (docs/design/platform-variants.md) ─────────

/** Routes a top-level class/object into its own emitted library file
 *  (`lib/<path>`) with its own import header, instead of `main.dart`.
 *  The unit of platform isolation: a `dart:io`-touching implementation
 *  annotated `@DartLibrary("platform/x_io.dart")` keeps `dart:io` out of
 *  every other library's import graph.
 */
final class DartLibrary(val path: String) extends StaticAnnotation

/** On a `@native` object that also has `@DartImport(path)`: generates
 *  `lib/<path>` as a Dart conditional-export switch over `@DartLibrary`
 *  variant files —
 *  `export '<default>' if (dart.library.io) '<io>' if (dart.library.js_interop) '<web>';`
 *  Each variant declares the same Dart-side API under the same emitted
 *  name (`@DartName`); the object's own members are the facade the app
 *  calls. Omitted axes fall back to `default`.
 */
final class DartVariants(
  val default: String,
  val io: String = "",
  val web: String = ""
) extends StaticAnnotation

// ─── JSON codec synthesis ──────────────────────────────────────────────────

/** Forces JSON codec synthesis (`fromJson`/`toJson`) for a case class
 *  or sealed hierarchy. Rarely needed: every case class whose fields are
 *  wire-shaped gets codecs WITHOUT this annotation, so shared model
 *  modules need not reference Sart. Use it to force synthesis on a type
 *  the heuristic would skip.
 */
final class JsonModel extends StaticAnnotation

/** Overrides the `type` discriminator a sealed-hierarchy member writes
 *  and matches in JSON, and the wire value of an enum member
 *  (`@JsonTag("movie") case object Movie` serialises as `"movie"` — the
 *  equivalent of json_serializable's `@JsonValue`). Defaults to fabric's
 *  convention: the capitalised tail of the fully-qualified name —
 *  `Outer.Inner` for a class nested in an object, the bare name at top
 *  level, `Parent.Member` for enum members.
 */
final class JsonTag(val tag: String) extends StaticAnnotation

/** Overrides the JSON key a model field reads/writes — the counterpart
 *  of json_serializable's `@JsonKey(name: '...')`. Defaults to the field
 *  name, so a field literally named `_id` needs nothing.
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
