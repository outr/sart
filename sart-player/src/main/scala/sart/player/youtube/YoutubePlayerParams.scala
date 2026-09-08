package sart.player.youtube

import sart.dart.*

/** Construction-time params for a [[YoutubePlayerController]]. */
@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
@DartPackage("youtube_player_iframe", "^5.2.0")
class YoutubePlayerParams(
  val showControls: Boolean = native.value,
  val showFullscreenButton: Boolean = native.value,
  val enableCaption: Boolean = native.value,
  val strictRelatedVideos: Boolean = native.value,
  val mute: Boolean = native.value
) extends DartObject
