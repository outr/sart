package sart.player

import sart.stdlib.Stream
import sart.player.mediakit.MediaKit
import flutter.material.Widget
import scala.concurrent.Future

/** Cross-platform video surface — the faithful, Nabo-specific-stripped port
 *  of NaboPlayer's `NaboVideo` interface. Backends: media_kit (libmpv) on
 *  Android / iOS / desktop, an hls.js `<video>` on web. All expose this one
 *  interface, selected by [[VideoPlayer.create]].
 *
 *  Dart `set x(v)` members are modelled as `setX(v)` methods (Sart emits Dart
 *  setters from `var` fields, not from user-declared setter methods).
 */
trait VideoPlayer:
  /** Move playback between the hardware video plane and the graphics layer,
   *  where the backend has such a distinction (Android/ExoPlayer). A no-op
   *  elsewhere, so it's concrete here — web/media_kit have nothing to swap. */
  def setTextureSurface(texture: Boolean): Unit = ()

  /** Ask a native view to re-run layout after being re-parented (Android
   *  only; a no-op elsewhere). */
  def relayout(): Unit = ()

  /** The profile's preferred audio / subtitle languages (ExoPlayer track
   *  selector; a no-op on backends that pick tracks their own way). */
  def setPreferredLanguages(audio: String, text: String): Unit = ()

  /** Scale the decoded frame's colour channels for Night Mode / picture
   *  adjustment (1.0 / 0.0 is the identity; a no-op with no frame pipeline). */
  def setVideoAdjust(brightness: Double, warmth: Double): Unit = ()

  def setOnEnded(cb: () => Unit): Unit
  def setOnError(cb: () => Unit): Unit

  /** The source has produced playable data. The default infers it from
   *  duration/position; backends with a real READY signal override it. */
  def ready: Boolean = duration > 0 || position > 0

  /** One line for the opening overlay while a source is retried, or none. */
  def statusNote: Option[String] = Option.empty

  /** True while the player has a source but is waiting on data. */
  def buffering: Boolean = false

  /** What actually decoded, once the backend knows — or none while unknown. */
  def videoSize: Option[VideoSize] = Option.empty

  def setSource(
    url: String,
    startSeconds: Double = 0.0,
    sideloaded: List[SubtitleSource] = Nil
  ): Future[Unit]
  def addSubtitles(subs: List[SubtitleSource]): Future[Unit]

  /** True when the engine renders image-based (PGS/VobSub) subtitles itself,
   *  so the picker can select them directly (ExoPlayer: true; else false). */
  def rendersImageSubtitles: Boolean

  def subtitleTracks: List[SubtitleTrackInfo]
  def currentSubtitleId: Option[String]
  def subtitleTracksStream: Stream[Unit]
  def selectEmbeddedSubtitle(id: String): Unit
  def selectUriSubtitle(
    url: String,
    label: Option[String] = Option.empty,
    language: Option[String] = Option.empty
  ): Unit
  def subtitlesOff(): Unit

  def audioTracks: List[AudioTrackInfo]
  def currentAudioId: Option[String]
  def audioTracksStream: Stream[Unit]
  def selectAudioTrack(id: String): Unit

  def seek(seconds: Double): Unit
  def play(): Unit
  def pause(): Unit

  /** Playback speed multiplier (1.0 = normal). */
  def setRate(v: Double): Unit
  /** 0..1. Set to 0 for a muted (ambient/background) video. */
  def setVolume(v: Double): Unit
  /** Per-title volume multiplier on top of [[setVolume]] (1.0 unchanged,
   *  <1 attenuates, >1 boosts up to 2.0 via a native path per backend). */
  def setGain(g: Double): Unit
  /** Restart automatically on completion (looping ambient/background video). */
  def setLooping(v: Boolean): Unit
  /** Scale the video to FILL its box, cropping overflow (BoxFit.cover). */
  def setCover(v: Boolean): Unit
  /** Server-persisted subtitle track id to restore on load (before setSource). */
  def setPreferredSubtitleId(id: Option[String]): Unit
  /** `Authorization` header to send when fetching the stream (before setSource;
   *  web only — a no-op where the token rides in the URL). */
  def setAuthHeader(v: Option[String]): Unit
  /** Request/hold audio focus so this player owns the audio stream (Android;
   *  a no-op where the platform has no focus concept). */
  def setAudioFocus(v: Boolean): Unit
  /** Audio-only playback: the screen renders a visualizer instead of the video
   *  surface (and on Android installs a live PCM tap; see [[audioBands]]). */
  def setAudioOnly(v: Boolean): Unit

  /** Live normalized band magnitudes (0..1) from the native PCM tap for the
   *  visualizer — none where no live tap exists (media_kit/web). */
  def audioBands: Option[Stream[List[Double]]]

  def position: Double
  def duration: Double
  def paused: Boolean
  def ended: Boolean

  def dispose(): Unit
  def view(): Widget

object VideoPlayer:
  /** The platform's video backend. media_kit on Android / iOS / desktop; the
   *  hls.js `<video>` on web is selected by the `@DartVariants` conditional
   *  export in a later phase. */
  def create(): VideoPlayer =
    MediaKit.ensureInitialized() // load libmpv before the first Player (idempotent)
    MediaKitVideo()
