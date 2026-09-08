package sart.player.youtube

import sart.dart.*
import sart.stdlib.Duration

/** Metadata for the currently-loaded YouTube video. */
@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
@DartPackage("youtube_player_iframe", "^5.2.0")
class YoutubeMetaData extends DartObject:
  def duration: Duration = native.value
