package sart.player.mediakit

import sart.dart.*

/** libmpv log verbosity for [[PlayerConfiguration]]. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class MPVLogLevel extends DartObject

@native
@DartImport("package:media_kit/media_kit.dart")
object MPVLogLevel:
  def info: MPVLogLevel = native.value
  def warn: MPVLogLevel = native.value
  def error: MPVLogLevel = native.value
  def debug: MPVLogLevel = native.value
