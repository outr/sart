package flutter

import sart.dart.*
import flutter.material.Widget

// Top-level `runApp` is a free function in Dart. We expose it on a Scala
// object the app can import. The emitter special-cases this: calls to
// `runApp.apply(w)` become `runApp(w)` in the generated Dart, with no
// reference to the enclosing Scala object.
@native
@DartImport("package:flutter/material.dart")
object runApp:
  def apply(widget: Widget): Unit = native.value
