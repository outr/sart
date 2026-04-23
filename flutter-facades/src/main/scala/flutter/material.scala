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

@native
@DartImport("package:flutter/material.dart")
class TextTheme extends DartObject:
  def headlineMedium: TextStyle = native.value

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
