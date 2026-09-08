package sart.tv.media

import sart.dart.*
import sart.stdlib.Duration
import scala.concurrent.Future

/** Transport commands the OS delivers back to the app — from the lock
 *  screen, notification, Bluetooth/steering-wheel media buttons, CarPlay
 *  and Android Auto. Every callback defaults to a no-op; supply the ones
 *  your player acts on. */
case class MediaCallbacks(
  onPlay: () => Unit = null,
  onPause: () => Unit = null,
  onStop: () => Unit = null,
  onSeek: Duration => Unit = null,
  onSkipToNext: () => Unit = null,
  onSkipToPrevious: () => Unit = null,
  onFastForward: () => Unit = null,
  onRewind: () => Unit = null
)

/** A [[BaseAudioHandler]] wired to a [[MediaCallbacks]]: it forwards each
 *  OS transport command to the matching callback and keeps the OS UI in
 *  sync. Apps rarely construct this directly — [[MediaSession.init]] does.
 *  Push now-playing metadata with [[setNowPlaying]] and playback state
 *  with [[setPlaying]] / [[setPosition]]. */
class SartAudioHandler(private val callbacks: MediaCallbacks) extends BaseAudioHandler:
  private var playing: Boolean = false
  private var position: Duration = Duration()

  // Each callback is optional (null when the app didn't supply it); bind
  // to a local so Dart promotes it to non-null across the guard.
  override def play(): Future[Unit] =
    val cb = callbacks.onPlay
    if cb != null then cb()
    playing = true
    broadcast()
    Future.successful(())

  override def pause(): Future[Unit] =
    val cb = callbacks.onPause
    if cb != null then cb()
    playing = false
    broadcast()
    Future.successful(())

  override def stop(): Future[Unit] =
    val cb = callbacks.onStop
    if cb != null then cb()
    playing = false
    broadcast()
    Future.successful(())

  override def seek(pos: Duration): Future[Unit] =
    val cb = callbacks.onSeek
    if cb != null then cb(pos)
    position = pos
    broadcast()
    Future.successful(())

  override def skipToNext(): Future[Unit] =
    val cb = callbacks.onSkipToNext
    if cb != null then cb()
    Future.successful(())

  override def skipToPrevious(): Future[Unit] =
    val cb = callbacks.onSkipToPrevious
    if cb != null then cb()
    Future.successful(())

  override def fastForward(): Future[Unit] =
    val cb = callbacks.onFastForward
    if cb != null then cb()
    Future.successful(())

  override def rewind(): Future[Unit] =
    val cb = callbacks.onRewind
    if cb != null then cb()
    Future.successful(())

  /** Publish now-playing metadata: what the OS shows on the lock screen. */
  def setNowPlaying(item: MediaItem): Unit = mediaItem.add(item)

  /** Update the playing flag and refresh the OS transport controls. */
  def setPlaying(isPlaying: Boolean): Unit =
    playing = isPlaying
    broadcast()

  /** Update the position shown on the OS scrubber. */
  def setPosition(pos: Duration): Unit =
    position = pos
    broadcast()

  private def broadcast(): Unit =
    playbackState.add(
      PlaybackState(
        processingState = AudioProcessingState.ready,
        playing = playing,
        controls = List(
          MediaControl.rewind,
          if playing then MediaControl.pause else MediaControl.play,
          MediaControl.stop,
          MediaControl.fastForward
        ),
        systemActions = Set(MediaAction.seek),
        updatePosition = position
      )
    )

/** OS media-session entry point. Cross-platform over Apple TV, Android TV,
 *  Tizen, webOS, mobile and desktop via the `audio_service` package.
 *
 *  {{{
 *  val session = await(MediaSession.init(
 *    MediaCallbacks(onPlay = () => player.play(), onPause = () => player.pause()),
 *    AudioServiceConfig(
 *      androidNotificationChannelId = "com.example.audio",
 *      androidNotificationChannelName = "Playback"
 *    )
 *  ))
 *  session.setNowPlaying(MediaItem(id = "1", title = "Episode 1", artist = "Show"))
 *  session.setPlaying(true)
 *  }}}
 *
 *  Platform setup is the usual `audio_service` wiring (Android: an
 *  `<service>`/`<receiver>` in the manifest; iOS/tvOS: the `audio`
 *  background mode) — see the package docs. */
object MediaSession:
  /** Initialise the media session once, before `runApp`. */
  def init(
    callbacks: MediaCallbacks,
    config: AudioServiceConfig = AudioServiceConfig()
  ): Future[SartAudioHandler] =
    // audio_service's init takes named params.
    AudioService.init(builder = () => SartAudioHandler(callbacks), config = config)
