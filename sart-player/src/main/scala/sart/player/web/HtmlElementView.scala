package sart.player.web

import sart.dart.*
import flutter.material.{Widget, Key}

/** Flutter widget that mounts a registered platform-view DOM element by its
 *  [[viewType]] id. */
@native
@DartImport("package:flutter/widgets.dart")
class HtmlElementView(val viewType: String, val key: Key = native.value) extends Widget
