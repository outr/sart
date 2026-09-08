package sart.player.youtube

import sart.dart.*
import flutter.material.{Widget, Key}

/** The IFrame player render widget for a [[YoutubePlayerController]]. */
@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
@DartPackage("youtube_player_iframe", "^5.2.0")
class YoutubePlayer(
  val controller: YoutubePlayerController,
  val aspectRatio: Double = native.value,
  val key: Key = native.value
) extends Widget
