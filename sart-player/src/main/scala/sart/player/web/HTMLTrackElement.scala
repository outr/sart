package sart.player.web

import sart.dart.*

/** A `<track>` element (package:web) — a subtitle/caption track. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class HTMLTrackElement() extends Node:
  var kind: String = native.value
  var src: String = native.value
  var srclang: String = native.value
  var label: String = native.value
  def track: TextTrack = native.value
  def setAttribute(name: String, value: String): Unit = native.value
