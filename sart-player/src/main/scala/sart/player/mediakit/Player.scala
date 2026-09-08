package sart.player.mediakit

import sart.dart.*
import sart.stdlib.Duration
import scala.concurrent.Future

/** libmpv-backed player (media_kit). Owns decode + render; exposes state
 *  synchronously via [[state]] and reactively via [[stream]]. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class Player(
  val configuration: PlayerConfiguration = native.value
) extends DartObject:
  def state: PlayerState = native.value
  def stream: PlayerStream = native.value

  def open(media: Media, play: Boolean = native.value): Future[Unit] = native.value
  def play(): Future[Unit] = native.value
  def pause(): Future[Unit] = native.value
  def seek(duration: Duration): Future[Unit] = native.value
  def setVolume(volume: Double): Future[Unit] = native.value
  def setRate(rate: Double): Future[Unit] = native.value
  def setPlaylistMode(mode: PlaylistMode): Future[Unit] = native.value
  def setSubtitleTrack(track: SubtitleTrack): Future[Unit] = native.value
  def setAudioTrack(track: AudioTrack): Future[Unit] = native.value
  def dispose(): Future[Unit] = native.value
