package sart.player.youtube

import sart.dart.*

/** A state update from the controller's stream. */
@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
@DartPackage("youtube_player_iframe", "^5.2.0")
class YoutubePlayerValue extends DartObject:
  def playerState: PlayerState = native.value
  def metaData: YoutubeMetaData = native.value
  def error: YoutubeError = native.value
