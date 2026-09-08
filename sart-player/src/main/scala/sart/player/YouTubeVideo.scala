package sart.player

import sart.dart.*
import sart.stdlib.{Stream, StreamController, StreamSubscription}
import sart.player.youtube.{
  YoutubePlayerController, YoutubePlayerParams, YoutubePlayerValue, YoutubeVideoState,
  YoutubePlayer, PlayerState, YoutubeError
}
import flutter.material.Widget
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** YouTube playback as a [[VideoPlayer]], so it plugs into the same controls
 *  as the other backends. Over `youtube_player_iframe` (v5, cross-platform:
 *  a real `<iframe>` on web, a WebView on Android/iOS). `setSource(url)` takes
 *  a YouTube videoId, not a stream URL. Faithful port of NaboPlayer's web
 *  `YouTubeVideo`, Nabo-specific coupling removed.
 */
class YouTubeVideo(muted: Boolean = false, captions: Boolean = true) extends VideoPlayer:
  private var controller: YoutubePlayerController = null
  private var onEndedCb: Option[() => Unit] = None
  private var onErrorCb: Option[() => Unit] = None
  private var endedFired: Boolean = false
  private var looping: Boolean = false
  private var currentId: Option[String] = None
  private var positionSec: Double = 0.0
  private var durationSec: Double = 0.0
  private var pausedState: Boolean = true
  private var stateSub: StreamSubscription[YoutubePlayerValue] = null
  private var videoStateSub: StreamSubscription[YoutubeVideoState] = null
  // Interface streams YouTube doesn't drive (no sideloaded/embedded tracks).
  private val idleCtrl: StreamController[Unit] = StreamController.broadcast[Unit]()

  // Constructor body (runs after field initialisers, where `this` is available).
  controller = YoutubePlayerController(
    params = YoutubePlayerParams(
      showControls = false,
      showFullscreenButton = false,
      enableCaption = captions,
      strictRelatedVideos = true,
      mute = muted
    )
  )
  stateSub = controller.stream.listen(v =>
    pausedState = v.playerState != PlayerState.playing
    val dur = v.metaData.duration.inMilliseconds / 1000.0
    if dur > 0 then durationSec = dur
    if v.playerState == PlayerState.ended then
      if looping && currentId.isDefined then
        controller.loadVideoById(videoId = currentId.get)
      else if !endedFired then
        endedFired = true
        onEndedCb.foreach(cb => cb())
    if v.error != YoutubeError.none then onErrorCb.foreach(cb => cb())
  )
  videoStateSub = controller.videoStateStream.listen(s =>
    positionSec = s.position.inMilliseconds / 1000.0
  )

  override def setOnEnded(cb: () => Unit): Unit = onEndedCb = Some(cb)
  override def setOnError(cb: () => Unit): Unit = onErrorCb = Some(cb)

  override def rendersImageSubtitles: Boolean = false

  /** `url` is a YouTube videoId here (not a stream URL). */
  override def setSource(
    url: String,
    startSeconds: Double = 0.0,
    sideloaded: List[SubtitleSource] = Nil
  ): Future[Unit] =
    endedFired = false
    currentId = Some(url)
    await(controller.loadVideoById(videoId = url, startSeconds = startSeconds))
    Future.successful(())

  override def addSubtitles(subs: List[SubtitleSource]): Future[Unit] = Future.successful(())
  override def subtitleTracks: List[SubtitleTrackInfo] = Nil
  override def currentSubtitleId: Option[String] = None
  override def subtitleTracksStream: Stream[Unit] = idleCtrl.stream
  override def selectEmbeddedSubtitle(id: String): Unit = ()
  override def selectUriSubtitle(
    url: String,
    label: Option[String] = Option.empty,
    language: Option[String] = Option.empty
  ): Unit = ()
  override def subtitlesOff(): Unit = ()

  override def audioTracks: List[AudioTrackInfo] = Nil
  override def currentAudioId: Option[String] = None
  override def audioTracksStream: Stream[Unit] = idleCtrl.stream
  override def selectAudioTrack(id: String): Unit = ()

  override def seek(seconds: Double): Unit = controller.seekTo(seconds = seconds)
  override def setVolume(v: Double): Unit = controller.setVolume((v * 100).round.toInt)
  // YouTube has no per-title gain store; reuse the 0..100 volume path.
  override def setGain(g: Double): Unit = controller.setVolume((g * 100).round.toInt)
  override def setRate(v: Double): Unit = controller.setPlaybackRate(v)
  override def setLooping(v: Boolean): Unit = looping = v // on ended we reload the same id
  override def setCover(v: Boolean): Unit = () // a 16:9 iframe already fills a 16:9 surface
  override def setPreferredSubtitleId(id: Option[String]): Unit = ()
  override def setAuthHeader(v: Option[String]): Unit = ()
  override def setAudioFocus(v: Boolean): Unit = ()
  override def setAudioOnly(v: Boolean): Unit = ()
  override def audioBands: Option[Stream[List[Double]]] = Option.empty

  override def play(): Unit = controller.playVideo()
  override def pause(): Unit = controller.pauseVideo()

  override def position: Double = positionSec
  override def duration: Double = durationSec
  override def paused: Boolean = pausedState
  override def ended: Boolean = endedFired
  // The embed reports an adaptive quality label, not a frame size — none by design.
  override def videoSize: Option[VideoSize] = Option.empty

  override def dispose(): Unit =
    // stateSub/videoStateSub are assigned in the ctor, so always non-null here.
    stateSub.cancel()
    videoStateSub.cancel()
    if !idleCtrl.isClosed then idleCtrl.close()
    controller.close()

  override def view(): Widget =
    YoutubePlayer(controller = controller, aspectRatio = 16.0 / 9.0)
