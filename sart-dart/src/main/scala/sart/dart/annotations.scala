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
