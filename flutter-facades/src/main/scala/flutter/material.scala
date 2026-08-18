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
trait SingleTickerProviderStateMixin extends DartObject

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
  def findRenderObject(): DartObject = native.value

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
  def pop(result: Any): Unit = native.value
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
  def connectionState: ConnectionState      = native.value

// ─── Text editing ──────────────────────────────────────────────────────────

// Kept curated: `clear`/`dispose` are inherited from ValueNotifier /
// ChangeNotifier, which the generator's type-collapse folds away.
// `text` is a var: Dart exposes a setter (`controller.text = "..."`).
@native
@DartImport("package:flutter/material.dart")
class TextEditingController extends DartObject:
  var text: String = native.value
  var selection: flutter.services.TextSelection = native.value
  def clear(): Unit = native.value
  def dispose(): Unit = native.value
  def addListener(listener: () => Unit): Unit = native.value
  def removeListener(listener: () => Unit): Unit = native.value

// ─── Keys / semantics (curated: generic parent chains the generator
// can't bridge — ValueKey<T> → LocalKey → Key; Semantics extends a
// private _SemanticsBase) ──────────────────────────────────────────────────
@native
@DartImport("package:flutter/material.dart")
class ValueKey[T](val value: T) extends Key

// Curated for the same reason as ValueKey (generic chain through
// ValueKey<T> → LocalKey → Key).
@native
@DartImport("package:flutter/material.dart")
class PageStorageKey[T](val value: T) extends Key

@native
@DartImport("package:flutter/material.dart")
class Semantics(
  val key: Key = native.value,
  val child: Widget = native.value,
  val container: Boolean = native.value,
  val label: String = native.value,
  val tooltip: String = native.value,
  val enabled: Boolean = native.value,
  val button: Boolean = native.value,
  val selected: Boolean = native.value,
  val textField: Boolean = native.value
) extends StatelessWidget

// Curated: RichText's real parent chain (MultiChildRenderObjectWidget)
// defeats the generator.
@native
@DartImport("package:flutter/material.dart")
class RichText(
  val text: InlineSpan,
  val overflow: TextOverflow = native.value,
  val maxLines: Int = native.value,
  val softWrap: Boolean = native.value,
  val textAlign: TextAlign = native.value,
  val key: Key = native.value
) extends Widget

// Curated: the generator renders StateSetter (`void Function(void
// Function())`) with broken arrow associativity.
@native
@DartImport("package:flutter/material.dart")
class StatefulBuilder(
  val builder: (BuildContext, (() => Unit) => Unit) => Widget,
  val key: Key = native.value
) extends StatefulWidget

// flutter/foundation's top-level odds and ends (re-exported through
// material).
@native
@DartImport("package:flutter/foundation.dart")
@DartTopLevel
object foundation:
  def debugPrint(message: String): Unit = native.value
  def kIsWeb: Boolean = native.value

// ─── Canvas / painting (curated: dart:ui types with cascade-style
// mutation and subclassable CustomPainter) ─────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class Paint() extends DartObject:
  var color: Color = native.value
  var strokeWidth: Double = native.value
  var style: PaintingStyle = native.value

@native
@DartImport("package:flutter/material.dart")
class PaintingStyle extends DartObject

@native
@DartImport("package:flutter/material.dart")
object PaintingStyle:
  val fill: PaintingStyle = native.value
  val stroke: PaintingStyle = native.value

@native
@DartImport("package:flutter/material.dart")
class Path() extends DartObject:
  def moveTo(x: Double, y: Double): Unit = native.value
  def lineTo(x: Double, y: Double): Unit = native.value
  def close(): Unit = native.value

@native
@DartImport("package:flutter/material.dart")
class Canvas extends DartObject:
  def drawLine(p1: Offset, p2: Offset, paint: Paint): Unit = native.value
  def drawPath(path: Path, paint: Paint): Unit = native.value
  def drawCircle(c: Offset, radius: Double, paint: Paint): Unit = native.value
  def drawRect(rect: DartObject, paint: Paint): Unit = native.value

@native
@DartImport("package:flutter/material.dart")
abstract class CustomPainter extends DartObject:
  def paint(canvas: Canvas, size: Size): Unit = native.value
  def shouldRepaint(oldDelegate: CustomPainter): Boolean = native.value

// The repaint-boundary render object (PNG capture). `toImage` hands
// back a dart:ui Image (see flutter.ui).
@native
@DartImport("package:flutter/rendering.dart")
class RenderRepaintBoundary extends DartObject:
  def toImage(pixelRatio: Double = native.value): scala.concurrent.Future[flutter.ui.Image] = native.value

// vector_math_64's Matrix4 (re-exported through flutter/widgets).
@native
@DartImport("package:flutter/material.dart")
class Matrix4 extends DartObject:
  def translateByDouble(x: Double, y: Double, z: Double, w: Double): Unit = native.value
  def scaleByDouble(x: Double, y: Double, z: Double, w: Double): Unit = native.value

@native
@DartImport("package:flutter/material.dart")
object Matrix4:
  def identity(): Matrix4 = native.value

// Curated: `value` is a settable property (the generator folds it into a
// ctor val).
@native
@DartImport("package:flutter/material.dart")
class TransformationController() extends DartObject:
  var value: Matrix4 = native.value
  def toScene(viewportPoint: Offset): Offset = native.value
  def dispose(): Unit = native.value

// Animation driver — curated so `vsync = this` (a TickerProvider mixin
// receiver) types without the mixin surface.
@native
@DartImport("package:flutter/material.dart")
class AnimationController(
  val vsync: Any,
  val duration: sart.stdlib.Duration = native.value
) extends DartObject:
  def addListener(listener: () => Unit): Unit = native.value
  def removeListener(listener: () => Unit): Unit = native.value
  def repeat(): DartObject = native.value
  def stop(): Unit = native.value
  def dispose(): Unit = native.value
  def isAnimating: Boolean = native.value

// Pointer/gesture payloads reach facades as DartObject — cast to these.
@native
@DartImport("package:flutter/material.dart")
class PointerEvent extends DartObject:
  def buttons: Int = native.value
  def localPosition: Offset = native.value
  def localDelta: Offset = native.value
  def delta: Offset = native.value
  def position: Offset = native.value

@native
@DartImport("package:flutter/material.dart")
class TapUpDetails extends DartObject:
  def localPosition: Offset = native.value

@native
@DartImport("package:flutter/gestures.dart")
@DartTopLevel
object gestures:
  def kPrimaryButton: Int = native.value

// ─── Material colors ───────────────────────────────────────────────────────

// Curated: the real chain is MaterialColor → ColorSwatch<int> → Color; the
// generator can't bridge the generic middle link, and `Colors.grey` et al.
// must remain assignable wherever a Color is expected.
@native
@DartImport("package:flutter/material.dart")
class MaterialColor extends Color(0):
  def shade50: Color = native.value
  def shade100: Color = native.value
  def shade200: Color = native.value
  def shade300: Color = native.value
  def shade400: Color = native.value
  def shade500: Color = native.value
  def shade600: Color = native.value
  def shade700: Color = native.value
  def shade800: Color = native.value
  def shade900: Color = native.value

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
