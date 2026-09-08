package sart.player.web

import sart.dart.*

/** A `<script>` element (package:web) — used to inject hls.js. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class HTMLScriptElement() extends Node:
  var src: String = native.value
  var async: Boolean = native.value
