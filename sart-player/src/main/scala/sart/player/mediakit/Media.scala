package sart.player.mediakit

import sart.dart.*

/** A media source (URL or file) for [[Player.open]]. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class Media(val uri: String) extends DartObject
