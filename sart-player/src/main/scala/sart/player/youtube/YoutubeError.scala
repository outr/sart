package sart.player.youtube

import sart.dart.*

/** youtube_player_iframe error state; none means error-free. */
@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
@DartPackage("youtube_player_iframe", "^5.2.0")
class YoutubeError extends DartObject

@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
object YoutubeError:
  def none: YoutubeError = native.value
