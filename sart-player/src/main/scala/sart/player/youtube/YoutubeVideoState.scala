package sart.player.youtube

import sart.dart.*
import sart.stdlib.Duration

/** A position update from the controller's videoStateStream. */
@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
@DartPackage("youtube_player_iframe", "^5.2.0")
class YoutubeVideoState extends DartObject:
  def position: Duration = native.value
