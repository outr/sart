package example.player

import sart.dart.*
import flutter.material.*
import scala.concurrent.Future

/** Facade over the cross-platform `video_player` package (web, Android,
 *  iOS, desktop). This is the general-purpose player primitive — the
 *  spike proving a Sart-authored player plays real media. A production
 *  `sart-player` library would wrap this (and media_kit for wider codec
 *  support) behind a platform-variant `Player` interface.
 */
@native
@DartImport("package:video_player/video_player.dart")
@DartPackage("video_player", "^2.9.2")
class VideoPlayerController(val key: Key = native.value) extends DartObject:
  def initialize(): Future[Unit] = native.value
  def play(): Future[Unit] = native.value
  def pause(): Future[Unit] = native.value
  def setLooping(looping: Boolean): Future[Unit] = native.value
  def setVolume(volume: Double): Future[Unit] = native.value
  def dispose(): Future[Unit] = native.value
  def value: VideoPlayerValue = native.value

@native
@DartImport("package:video_player/video_player.dart")
object VideoPlayerController:
  /** `VideoPlayerController.networkUrl(Uri.parse(url))`. */
  def networkUrl(url: sart.stdlib.Uri): VideoPlayerController = native.value  // Uri from sart.stdlib
  /** `VideoPlayerController.asset('assets/sample.mp4')`. */
  def asset(name: String): VideoPlayerController = native.value

@native
@DartImport("package:video_player/video_player.dart")
class VideoPlayerValue extends DartObject:
  def isInitialized: Boolean = native.value
  def isPlaying: Boolean = native.value
  def position: sart.stdlib.Duration = native.value
  def duration: sart.stdlib.Duration = native.value
  def aspectRatio: Double = native.value

/** The video render widget: `VideoPlayer(controller)`. */
@native
@DartImport("package:video_player/video_player.dart")
class VideoPlayer(val controller: VideoPlayerController) extends StatelessWidget
