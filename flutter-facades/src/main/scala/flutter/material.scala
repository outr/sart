package flutter.material

import sart.dart.*

// NOTE on design:
// - Everything here is `@native` — these classes exist only so user code
//   has real types to compile against. The Sart compiler never emits Dart
//   for anything in this file; it passes the names through.
// - This file holds only the CURATED core: declarations that carry
//   semantic choices the facade generator can't make — the widget/state
//   subclassing surface (`State[W]`'s type parameter), Option-mapped
//   `AsyncSnapshot`, the pubspec annotations on `Widget`, and types whose
//   useful members are inherited from parents the generator collapses
//   away (`TextEditingController`). The bulk of the material surface is
//   GENERATED from the Flutter SDK into `material_generated.scala` —
//   regenerate with `sbt sartFacadesRegen` (manifest: facadegen.conf).
// - Each top-level facade could carry its own `@DartPackage`, but since
//   everything transitively reaches `Widget`, we only declare the pubspec
//   dep once, on `Widget`.
// - Constructor parameters are typed with real Scala types and default to
//   `native.value` (a `Nothing`-returning sentinel). That lets callers use
//   Dart-style named arguments without supplying every parameter.

@native
@DartImport("package:flutter/material.dart")
@DartPackage("flutter", sdk = "flutter")
@DartPubspec("""flutter:
  uses-material-design: true
""")
abstract class Widget extends DartObject

@native
@DartImport("package:flutter/material.dart")
abstract class StatelessWidget extends Widget:
  def build(context: BuildContext): Widget = native.value

@native
@DartImport("package:flutter/material.dart")
abstract class StatefulWidget extends Widget:
  def createState(): State[? <: StatefulWidget] = native.value

@native
@DartImport("package:flutter/material.dart")
abstract class State[W <: StatefulWidget] extends DartObject:
  def widget: W = native.value
  def context: BuildContext = native.value
  def mounted: Boolean = native.value
  def setState(fn: () => Unit): Unit = native.value
  def build(context: BuildContext): Widget = native.value
  // Lifecycle — overrides must call super.initState() / super.dispose(),
  // exactly as in Dart.
  def initState(): Unit = native.value
  def dispose(): Unit = native.value
  def didChangeDependencies(): Unit = native.value

// ─── State mixins ──────────────────────────────────────────────────────────

// Curated: Dart mixins aren't collected by the facade generator. These
// exist so Scala `with`-clauses emit the matching Dart mixin application.
@native
@DartImport("package:flutter/material.dart")
trait TickerProviderStateMixin extends DartObject

@native
@DartImport("package:flutter/material.dart")
trait AutomaticKeepAliveClientMixin extends DartObject:
  // Overriders write `override def wantKeepAlive: Boolean = true` — the
  // paren-less def emits as the Dart getter the mixin requires.
  def wantKeepAlive: Boolean = native.value

@native
@DartImport("package:flutter/material.dart")
class BuildContext extends DartObject:
  def mounted: Boolean = native.value

@native
@DartImport("package:flutter/material.dart")
class Key extends DartObject

// ─── Navigation ────────────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class Route[T] extends DartObject

@native
@DartImport("package:flutter/material.dart")
class MaterialPageRoute[T](
  val builder: BuildContext => Widget
) extends Route[T]

@native
@DartImport("package:flutter/material.dart")
class NavigatorState extends DartObject:
  def push[T](route: Route[T]): DartObject = native.value
  def pop(): Unit = native.value
  def pushReplacement[T](route: Route[T]): DartObject = native.value

@native
@DartImport("package:flutter/material.dart")
object Navigator:
  def of(context: BuildContext): NavigatorState = native.value

// ─── Images ────────────────────────────────────────────────────────────────

// ImageProvider is generic in Dart (`ImageProvider<Object>`); the facade
// stays unparameterised so `Image(image = NetworkImage(...))` type-checks
// without variance gymnastics.
@native
@DartImport("package:flutter/material.dart")
abstract class ImageProvider extends DartObject

@native
@DartImport("package:flutter/material.dart")
class NetworkImage(val url: String) extends ImageProvider

@native
@DartImport("package:flutter/material.dart")
class AssetImage(val name: String) extends ImageProvider

// ─── Async snapshots ───────────────────────────────────────────────────────

// Flutter's async-snapshot type — carries `data`, `error`, `connectionState`
// during Future/Stream subscriptions. `data` and `error` are nullable
// in the real Flutter API (the snapshot might not have produced a value
// yet), so we model them as `Option[T]` here — callers fold/get-or-else.
@native
@DartImport("package:flutter/material.dart")
class AsyncSnapshot[T] extends DartObject:
  def data: sart.stdlib.Option[T]          = native.value
  def hasData: Boolean                      = native.value
  def hasError: Boolean                     = native.value
  def error: sart.stdlib.Option[Object]     = native.value

// ─── Text editing ──────────────────────────────────────────────────────────

// Kept curated: `clear`/`dispose` are inherited from ValueNotifier /
// ChangeNotifier, which the generator's type-collapse folds away.
// `text` is a var: Dart exposes a setter (`controller.text = "..."`).
@native
@DartImport("package:flutter/material.dart")
class TextEditingController extends DartObject:
  var text: String = native.value
  def clear(): Unit = native.value
  def dispose(): Unit = native.value

// ─── Keys ──────────────────────────────────────────────────────────────────

// Curated: the real signature is `GlobalKey<T extends State<StatefulWidget>>`.
// `currentState`/`currentContext` are nullable in Dart, so they surface as
// Option — `key.currentState.get.validate()` emits the original's
// `key.currentState!.validate()` exactly.
@native
@DartImport("package:flutter/material.dart")
class GlobalKey[S] extends Key:
  def currentState: sart.stdlib.Option[S] = native.value
  def currentContext: sart.stdlib.Option[BuildContext] = native.value

// ─── Bindings ──────────────────────────────────────────────────────────────

// Curated: WidgetsBinding is a mixin composition in the SDK; all callers
// need is the post-frame hook off the singleton.
@native
@DartImport("package:flutter/material.dart")
class WidgetsBindingInstance extends DartObject:
  def addPostFrameCallback(fn: DartObject => Unit): Unit = native.value

@native
@DartImport("package:flutter/material.dart")
object WidgetsBinding:
  def instance: WidgetsBindingInstance = native.value
