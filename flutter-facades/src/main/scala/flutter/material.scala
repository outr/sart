package flutter.material

import sart.dart.*

// NOTE on design:
// - Everything here is `@native` — these classes exist only so the example
//   code has real types to compile against. The Sart compiler never emits
//   Dart for anything in this file; it passes the names through.
// - Each top-level facade could carry its own `@DartPackage`, but since
//   everything in the counter app transitively reaches `Widget`, we only
//   declare the pubspec dep once, on `Widget`.
// - Constructor parameters are typed with real Scala types and default to
//   `native.value` (a `Nothing`-returning sentinel). That lets callers use
//   Dart-style named arguments (`new Text(widget.title, style = ...)`)
//   without forcing every parameter to be supplied.

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
  def setState(fn: () => Unit): Unit = native.value
  def build(context: BuildContext): Widget = native.value

@native
@DartImport("package:flutter/material.dart")
class BuildContext extends DartObject

@native
@DartImport("package:flutter/material.dart")
class Key extends DartObject

@native
@DartImport("package:flutter/material.dart")
class MaterialApp(
  val title: String = native.value,
  val theme: ThemeData = native.value,
  val home: Widget = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class ThemeData(
  val colorScheme: ColorScheme = native.value,
  val useMaterial3: Boolean = native.value
) extends DartObject:
  def textTheme: TextTheme = native.value

@native
@DartImport("package:flutter/material.dart")
class ColorScheme extends DartObject:
  def inversePrimary: Color = native.value

@native
@DartImport("package:flutter/material.dart")
object ColorScheme:
  def fromSeed(seedColor: Color): ColorScheme = native.value

@native
@DartImport("package:flutter/material.dart")
class Color extends DartObject

@native
@DartImport("package:flutter/material.dart")
object Colors:
  val deepPurple: Color = native.value
  val red: Color   = native.value
  val green: Color = native.value
  val blue: Color  = native.value
  val black: Color = native.value
  val white: Color = native.value
  val grey: Color  = native.value
  val amber: Color = native.value
  val teal: Color  = native.value
  val transparent: Color = native.value

@native
@DartImport("package:flutter/material.dart")
object Color:
  // `Color.fromARGB(alpha, red, green, blue)` is Flutter's usual RGBA ctor.
  def fromARGB(a: Int, r: Int, g: Int, b: Int): Color = native.value
  def fromRGBO(r: Int, g: Int, b: Int, opacity: Double): Color = native.value

// ─── Decorations ───────────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class BorderRadius extends DartObject

@native
@DartImport("package:flutter/material.dart")
object BorderRadius:
  def circular(radius: Double): BorderRadius = native.value
  def all(radius: Double): BorderRadius = native.value

@native
@DartImport("package:flutter/material.dart")
class BoxShadow(
  val color: Color = native.value,
  val offset: Offset = native.value,
  val blurRadius: Double = native.value,
  val spreadRadius: Double = native.value
) extends DartObject

@native
@DartImport("package:flutter/material.dart")
class Offset(val dx: Double, val dy: Double) extends DartObject

@native
@DartImport("package:flutter/material.dart")
class BoxDecoration(
  val color: Color = native.value,
  val borderRadius: BorderRadius = native.value,
  val boxShadow: List[BoxShadow] = native.value
) extends DartObject

@native
@DartImport("package:flutter/material.dart")
class TextTheme extends DartObject:
  def headlineMedium: TextStyle = native.value
  def titleLarge: TextStyle = native.value
  def titleMedium: TextStyle = native.value
  def bodyMedium: TextStyle = native.value

@native
@DartImport("package:flutter/material.dart")
class TextStyle extends DartObject

@native
@DartImport("package:flutter/material.dart")
object Theme:
  def of(context: BuildContext): ThemeData = native.value

@native
@DartImport("package:flutter/material.dart")
class Scaffold(
  val appBar: AppBar = native.value,
  val body: Widget = native.value,
  val floatingActionButton: Widget = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class AppBar(
  val backgroundColor: Color = native.value,
  val title: Widget = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Text(
  val data: String,
  val style: TextStyle = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Center(
  val child: Widget = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Column(
  val mainAxisAlignment: MainAxisAlignment = native.value,
  val children: List[Widget] = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class MainAxisAlignment extends DartObject

@native
@DartImport("package:flutter/material.dart")
object MainAxisAlignment:
  val start: MainAxisAlignment = native.value
  val center: MainAxisAlignment = native.value
  val end: MainAxisAlignment = native.value

@native
@DartImport("package:flutter/material.dart")
class FloatingActionButton(
  val onPressed: () => Unit = native.value,
  val tooltip: String = native.value,
  val child: Widget = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class IconData extends DartObject

@native
@DartImport("package:flutter/material.dart")
class Icon(
  val icon: IconData
) extends Widget

@native
@DartImport("package:flutter/material.dart")
object Icons:
  val add: IconData = native.value
  val arrow_back: IconData = native.value
  val arrow_forward: IconData = native.value
  val check: IconData = native.value
  val close: IconData = native.value
  val delete: IconData = native.value
  val edit: IconData = native.value
  val menu: IconData = native.value

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

// ─── Stack layouts ─────────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class Stack(
  val children: List[Widget] = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Positioned(
  val child: Widget,
  val left: Double = native.value,
  val top: Double = native.value,
  val right: Double = native.value,
  val bottom: Double = native.value,
  val width: Double = native.value,
  val height: Double = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Alignment extends DartObject

@native
@DartImport("package:flutter/material.dart")
object Alignment:
  val topLeft: Alignment      = native.value
  val topCenter: Alignment    = native.value
  val topRight: Alignment     = native.value
  val centerLeft: Alignment   = native.value
  val center: Alignment       = native.value
  val centerRight: Alignment  = native.value
  val bottomLeft: Alignment   = native.value
  val bottomCenter: Alignment = native.value
  val bottomRight: Alignment  = native.value

@native
@DartImport("package:flutter/material.dart")
class Align(
  val alignment: Alignment = native.value,
  val child: Widget = native.value
) extends Widget

// ─── Images ────────────────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class Image(
  val image: ImageProvider
) extends Widget

@native
@DartImport("package:flutter/material.dart")
object Image:
  def network(url: String): Image = native.value
  def asset(name: String): Image  = native.value

@native
@DartImport("package:flutter/material.dart")
abstract class ImageProvider extends DartObject

@native
@DartImport("package:flutter/material.dart")
class NetworkImage(val url: String) extends ImageProvider

@native
@DartImport("package:flutter/material.dart")
class AssetImage(val name: String) extends ImageProvider

// ─── Gesture detection ─────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class GestureDetector(
  val onTap: () => Unit = native.value,
  val onDoubleTap: () => Unit = native.value,
  val onLongPress: () => Unit = native.value,
  val child: Widget = native.value
) extends Widget

// ─── Material elevation ────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class Card(
  val elevation: Double = native.value,
  val child: Widget = native.value
) extends Widget

// ─── Async builders ───────────────────────────────────────────────────────

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

@native
@DartImport("package:flutter/material.dart")
class FutureBuilder[T](
  val future: scala.concurrent.Future[T],
  val builder: (BuildContext, AsyncSnapshot[T]) => Widget
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class StreamBuilder[T](
  val stream: sart.stdlib.Stream[T],
  val builder: (BuildContext, AsyncSnapshot[T]) => Widget,
  val initialData: T = native.value
) extends Widget

// A generic Builder widget — useful for injecting a fresh BuildContext.
@native
@DartImport("package:flutter/material.dart")
class Builder(
  val builder: BuildContext => Widget
) extends Widget

// ─── Layout & spacing ──────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class EdgeInsets extends DartObject

@native
@DartImport("package:flutter/material.dart")
object EdgeInsets:
  def all(value: Double): EdgeInsets = native.value
  def symmetric(vertical: Double = native.value, horizontal: Double = native.value): EdgeInsets = native.value
  def only(
    left: Double = native.value,
    top: Double = native.value,
    right: Double = native.value,
    bottom: Double = native.value
  ): EdgeInsets = native.value

@native
@DartImport("package:flutter/material.dart")
class Padding(
  val padding: EdgeInsets,
  val child: Widget = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Expanded(
  val child: Widget,
  val flex: Int = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Flexible(
  val child: Widget,
  val flex: Int = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class SizedBox(
  val width: Double = native.value,
  val height: Double = native.value,
  val child: Widget = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Container(
  val padding: EdgeInsets = native.value,
  val margin: EdgeInsets = native.value,
  val width: Double = native.value,
  val height: Double = native.value,
  val color: Color = native.value,
  val decoration: BoxDecoration = native.value,
  val child: Widget = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Row(
  val mainAxisAlignment: MainAxisAlignment = native.value,
  val children: List[Widget] = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Divider extends Widget

// ─── Buttons ───────────────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class ElevatedButton(
  val onPressed: () => Unit,
  val child: Widget
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class TextButton(
  val onPressed: () => Unit,
  val child: Widget
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class IconButton(
  val onPressed: () => Unit,
  val icon: Widget,
  val tooltip: String = native.value
) extends Widget

// ─── Inputs ────────────────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class TextEditingController extends DartObject:
  def text: String = native.value
  def clear(): Unit = native.value
  def dispose(): Unit = native.value

@native
@DartImport("package:flutter/material.dart")
class InputDecoration(
  val labelText: String = native.value,
  val hintText: String = native.value,
  val border: DartObject = native.value
) extends DartObject

@native
@DartImport("package:flutter/material.dart")
class TextField(
  val controller: TextEditingController = native.value,
  val decoration: InputDecoration = native.value,
  val onChanged: String => Unit = native.value,
  val onSubmitted: String => Unit = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Checkbox(
  val value: Boolean,
  val onChanged: Boolean => Unit
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class Switch(
  val value: Boolean,
  val onChanged: Boolean => Unit
) extends Widget

// ─── Lists ─────────────────────────────────────────────────────────────────

@native
@DartImport("package:flutter/material.dart")
class ListTile(
  val leading: Widget = native.value,
  val title: Widget = native.value,
  val subtitle: Widget = native.value,
  val trailing: Widget = native.value,
  val onTap: () => Unit = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
class ListView(
  val padding: EdgeInsets = native.value,
  val children: List[Widget] = native.value
) extends Widget

@native
@DartImport("package:flutter/material.dart")
object ListView:
  def builder(
    itemCount: Int,
    itemBuilder: (BuildContext, Int) => Widget
  ): ListView = native.value
