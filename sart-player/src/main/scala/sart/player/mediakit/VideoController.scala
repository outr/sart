package sart.player.mediakit

import sart.dart.*

/** Bridges a [[Player]] to the [[Video]] render widget (media_kit_video). */
@native
@DartImport("package:media_kit_video/media_kit_video.dart")
@DartPackage("media_kit_video", "^2.0.1")
class VideoController(val player: Player) extends DartObject
