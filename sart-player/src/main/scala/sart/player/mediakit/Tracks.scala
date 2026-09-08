package sart.player.mediakit

import sart.dart.*

/** All tracks a [[Player]]'s current media exposes. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class Tracks extends DartObject:
  def subtitle: List[SubtitleTrack] = native.value
  def audio: List[AudioTrack] = native.value
