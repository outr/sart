package sart.player.web

import sart.dart.*

/** The browser console (`web.console`). */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class Console extends DartObject:
  def error(message: JSAny): Unit = native.value
