package sart.player.youtube

import sart.dart.*
import sart.stdlib.Stream
import scala.concurrent.Future

/** The youtube_player_iframe controller: loads videos by id and drives an
 *  embedded IFrame player (cross-platform in v5). */
@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
@DartPackage("youtube_player_iframe", "^5.2.0")
class YoutubePlayerController(
  val params: YoutubePlayerParams = native.value
) extends DartObject:
  def stream: Stream[YoutubePlayerValue] = native.value
  def videoStateStream: Stream[YoutubeVideoState] = native.value
  def loadVideoById(
    videoId: String = native.value,
    startSeconds: Double = native.value
  ): Future[Unit] = native.value
  def seekTo(seconds: Double = native.value, allowSeekAhead: Boolean = native.value): Future[Unit] = native.value
  def setVolume(volume: Int): Future[Unit] = native.value
  def setPlaybackRate(rate: Double): Future[Unit] = native.value
  def playVideo(): Future[Unit] = native.value
  def pauseVideo(): Future[Unit] = native.value
  def unMute(): Future[Unit] = native.value
  def close(): Future[Unit] = native.value
