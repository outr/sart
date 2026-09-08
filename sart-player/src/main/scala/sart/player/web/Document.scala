package sart.player.web

import sart.dart.*

/** The DOM document (`web.document`). */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class Document extends DartObject:
  def head: Option[Node] = native.value
