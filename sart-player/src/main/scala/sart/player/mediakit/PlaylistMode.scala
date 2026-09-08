package sart.player.mediakit

import sart.dart.*

/** Loop behaviour for [[Player.setPlaylistMode]]. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class PlaylistMode extends DartObject

@native
@DartImport("package:media_kit/media_kit.dart")
object PlaylistMode:
  def none: PlaylistMode = native.value
  def single: PlaylistMode = native.value
  def loop: PlaylistMode = native.value
