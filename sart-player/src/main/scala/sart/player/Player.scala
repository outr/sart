package sart.player

import sart.dart.*
import sart.stdlib.{Uri, Duration}
import flutter.material.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** A media source — a network URL or a bundled asset. */
sealed trait MediaSource
case class NetworkSource(url: String) extends MediaSource
case class AssetSource(name: String) extends MediaSource

/** A general-purpose, cross-platform video/audio player controller. Wraps
 *  the backend (currently `video_player`, cross-platform) behind a stable
 *  API — `Player(source)` in the widget tree, `controller.play()` etc.
 *  from callbacks — so a media_kit / hls.js backend can slot in behind
 *  the same interface via `@DartVariants` without touching call sites.
 */
class PlayerController(val source: MediaSource):
  private lazy val backend: VideoPlayerController = source match
    case NetworkSource(url) => VideoPlayerController.networkUrl(Uri.parse(url))
    case AssetSource(name)  => VideoPlayerController.asset(name)

  /** Load the media. Complete before the first frame renders. */
  def initialize(): Future[Unit] = backend.initialize()
  def play(): Future[Unit] = backend.play()
  def pause(): Future[Unit] = backend.pause()
  def seekTo(position: Duration): Future[Unit] = backend.seekTo(position)
  def setLooping(looping: Boolean): Future[Unit] = backend.setLooping(looping)
  def setVolume(volume: Double): Future[Unit] = backend.setVolume(volume)
  def dispose(): Future[Unit] = backend.dispose()

  def isPlaying: Boolean = backend.value.isPlaying
  def isInitialized: Boolean = backend.value.isInitialized
  def position: Duration = backend.value.position
  def duration: Duration = backend.value.duration
  def aspectRatio: Double = backend.value.aspectRatio

  /** The render widget for this controller's video (audio-only sources
   *  render nothing visible). */
  def view: Widget = VideoPlayer(backend)
