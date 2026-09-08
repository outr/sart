package sart.player.mediakit

import sart.dart.*

/** The currently-selected tracks on a [[Player]]. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class Track extends DartObject:
  def subtitle: SubtitleTrack = native.value
  def audio: AudioTrack = native.value
