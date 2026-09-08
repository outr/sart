package sart.player.mediakit

import sart.dart.*

/** Construction-time options for a [[Player]]. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class PlayerConfiguration(
  val logLevel: MPVLogLevel = native.value
) extends DartObject
