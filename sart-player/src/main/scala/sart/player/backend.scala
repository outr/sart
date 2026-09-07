package sart.player

import sart.dart.*
import sart.stdlib.{Uri, Duration}
import flutter.material.*
import scala.concurrent.Future

/** Facade over the `video_player` package — the cross-platform Flutter
 *  media backend (web, Android, iOS, macOS, Windows, Linux). This is the
 *  library's default backend; the public [[Player]] API wraps it so a
 *  media_kit-backed variant can slot in behind the same interface via
 *  `@DartVariants` without touching call sites.
 */
@native
@DartImport("package:video_player/video_player.dart")
@DartPackage("video_player", "^2.9.2")
class VideoPlayerController(val key: Key = native.value) extends DartObject:
  def initialize(): Future[Unit] = native.value
  def play(): Future[Unit] = native.value
  def pause(): Future[Unit] = native.value
  def seekTo(position: Duration): Future[Unit] = native.value
  def setLooping(looping: Boolean): Future[Unit] = native.value
  def setVolume(volume: Double): Future[Unit] = native.value
  def dispose(): Future[Unit] = native.value
  def value: VideoPlayerValue = native.value

@native
@DartImport("package:video_player/video_player.dart")
object VideoPlayerController:
  def networkUrl(url: Uri): VideoPlayerController = native.value
  def asset(name: String): VideoPlayerController = native.value

@native
@DartImport("package:video_player/video_player.dart")
class VideoPlayerValue extends DartObject:
  def isInitialized: Boolean = native.value
  def isPlaying: Boolean = native.value
  def isBuffering: Boolean = native.value
  def position: Duration = native.value
  def duration: Duration = native.value
  def aspectRatio: Double = native.value
  def volume: Double = native.value

@native
@DartImport("package:video_player/video_player.dart")
class VideoPlayer(val controller: VideoPlayerController) extends StatelessWidget
