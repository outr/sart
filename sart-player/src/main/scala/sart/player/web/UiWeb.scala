package sart.player.web

import sart.dart.*

/** dart:ui_web top-level `platformViewRegistry`. */
@native
@DartImport("dart:ui_web")
@DartTopLevel
object UiWeb:
  def platformViewRegistry: PlatformViewRegistry = native.value
