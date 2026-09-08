package sart.player.web

import sart.dart.*

/** Minimal DOM node (package:web) — enough to append children. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class Node extends DartObject:
  def appendChild(node: Node): Node = native.value
