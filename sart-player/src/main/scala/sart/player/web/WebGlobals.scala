package sart.player.web

import sart.dart.*

/** package:web top-level getters (`document`, `console`). */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
@DartTopLevel
object WebGlobals:
  def document: Document = native.value
  def console: Console = native.value
