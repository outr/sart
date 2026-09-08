package sart.player.web

import sart.dart.*

/** An element's inline CSS (`element.style`). Properties are settable strings. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class CSSStyleDeclaration extends DartObject:
  var width: String = native.value
  var height: String = native.value
  var backgroundColor: String = native.value
  var pointerEvents: String = native.value
  var objectFit: String = native.value
