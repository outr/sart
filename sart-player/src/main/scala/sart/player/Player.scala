package sart.player

import sart.dart.*
import sart.stdlib.{Uri, Duration}
import sart.tv.media.*
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

  // The OS media session, once bound. `mediaBound` guards access because
  // the handle is late-initialised only when the app opts in.
  private var media: SartAudioHandler = null
  private var mediaBound: Boolean = false

  /** Publish this player to the OS media session: `item` becomes the
   *  lock-screen / control-centre "now playing", and OS transport controls
   *  (remote, Bluetooth, CarPlay, Android Auto) drive playback through this
   *  controller. Call once after [[initialize]]. Playing/paused/seek state
   *  then stays in sync automatically as the app calls [[play]] / [[pause]]
   *  / [[seekTo]]. */
  def bindMediaSession(
    item: MediaItem,
    config: AudioServiceConfig = AudioServiceConfig()
  ): Future[Unit] =
    media = await(
      MediaSession.init(
        MediaCallbacks(
          onPlay = () => play(),
          onPause = () => pause(),
          onStop = () => pause(),
          onSeek = pos => seekTo(pos)
        ),
        config
      )
    )
    mediaBound = true
    media.setNowPlaying(item)
    media.setPosition(position)
    media.setPlaying(isPlaying)
    Future.successful(())

  /** Load the media. Complete before the first frame renders. */
  def initialize(): Future[Unit] = backend.initialize()
  def play(): Future[Unit] =
    if mediaBound then media.setPlaying(true)
    backend.play()
  def pause(): Future[Unit] =
    if mediaBound then media.setPlaying(false)
    backend.pause()
  def seekTo(position: Duration): Future[Unit] =
    if mediaBound then media.setPosition(position)
    backend.seekTo(position)
  def setLooping(looping: Boolean): Future[Unit] = backend.setLooping(looping)
  def setVolume(volume: Double): Future[Unit] = backend.setVolume(volume)
  def dispose(): Future[Unit] = backend.dispose()

  def isPlaying: Boolean = backend.value.isPlaying
  def isInitialized: Boolean = backend.value.isInitialized
  def position: Duration = backend.value.position
  def duration: Duration = backend.value.duration
  def aspectRatio: Double = backend.value.aspectRatio

  /** The render widget for this controller's video, sized to the video's
   *  native aspect ratio so it fills its box without letterbox artifacts.
   *  (Audio-only sources report a zero/one ratio and render nothing useful.) */
  def view: Widget =
    AspectRatio(aspectRatio = aspectRatio, child = VideoPlayer(backend))
