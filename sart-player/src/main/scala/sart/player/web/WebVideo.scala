package sart.player.web

import sart.dart.*
import sart.stdlib.{Stream, StreamController, clamp}
import sart.player.{VideoPlayer, VideoSize, SubtitleSource, SubtitleTrackInfo, AudioTrackInfo}
import flutter.material.Widget
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** Web backend: a `<video>` element mounted as a Flutter platform view, with
 *  hls.js loaded on demand for HLS on browsers without native HLS. Faithful
 *  port of NaboPlayer's web `NaboVideo`, Nabo-specific coupling removed.
 *
 *  Trimmed vs. the original, each pending a specific Sart js-interop gap
 *  (documented at the site): the `onEnded`/`onError` DOM event wiring
 *  (`.toJS` on typed callbacks), the embedded subtitle-track LISTING/selection
 *  (`TextTrackList` index operator), and the auth-header hls.js config
 *  (`dart:js_interop_unsafe`). Core playback + attaching sideloaded subtitle
 *  `<track>`s + basic hls.js are all live.
 */
@DartLibrary("platform/video_web.dart")
class WebVideo private (val viewType: String, val element: HTMLVideoElement, ref: ElementRef)
    extends VideoPlayer:
  private var hls: Option[Hls] = None
  private var authHeaderVal: Option[String] = None
  private var onEndedCb: Option[() => Unit] = None
  private var onErrorCb: Option[() => Unit] = None
  private val subCtrl: StreamController[Unit] = StreamController.broadcast[Unit]()
  private var volumeLevel: Double = 1.0
  private var gainLevel: Double = 1.0

  private[web] def bump(): Unit = if !subCtrl.isClosed then subCtrl.add(())

  // Callbacks are stored; wiring them to the <video> `ended`/`error` events
  // needs `.toJS` on a typed callback (a js-interop-authoring gap) — follow-up.
  override def setOnEnded(cb: () => Unit): Unit = onEndedCb = Some(cb)
  override def setOnError(cb: () => Unit): Unit = onErrorCb = Some(cb)

  override def rendersImageSubtitles: Boolean = false

  override def setSource(
    url: String,
    startSeconds: Double = 0.0,
    sideloaded: List[SubtitleSource] = Nil
  ): Future[Unit] =
    if sideloaded.nonEmpty then await(addSubtitles(sideloaded))
    val isHls = url.contains(".m3u8")
    val header = authHeaderVal
    val nativeHls = element.canPlayType("application/vnd.apple.mpegurl").nonEmpty
    if isHls && !nativeHls then
      if await(WebVideo.ensureHls()) then
        hls.foreach(h => h.destroy())
        // Auth-header HLS (the Emby/Jellyfin `xhrSetup` config) needs
        // dart:js_interop_unsafe getProperty/setProperty — follow-up; for now
        // hls.js runs with a default config (covers unauthenticated HLS).
        val h = Hls(JSObject())
        h.loadSource(url)
        h.attachMedia(element)
        hls = Some(h)
        return Future.successful(())
      else if header.isDefined then
        WebGlobals.console.error("[sart-player] hls.js failed to load".toJS)
    element.src = url
    Future.successful(())

  /** Attach each sideloaded subtitle as a `<track>` pointing at its URL. */
  override def addSubtitles(subs: List[SubtitleSource]): Future[Unit] =
    subs.foreach(s => attachTrack(s.url, s.label, s.language, s.isDefault, show = false))
    Future.successful(())

  // Reading the <video>'s embedded TextTrack list needs the `TextTrackList`
  // index operator (`tt[i]`), which Sart can't yet express on a facade —
  // follow-up. Attaching tracks (below) works; listing/selecting them is stubbed.
  override def subtitleTracks: List[SubtitleTrackInfo] = Nil
  override def currentSubtitleId: Option[String] = None
  override def subtitleTracksStream: Stream[Unit] = subCtrl.stream
  override def selectEmbeddedSubtitle(id: String): Unit = ()
  override def subtitlesOff(): Unit = ()

  override def selectUriSubtitle(
    url: String,
    label: Option[String] = Option.empty,
    language: Option[String] = Option.empty
  ): Unit = attachTrack(url, label, language, isDefault = false, show = true)

  private def attachTrack(
    url: String,
    label: Option[String],
    language: Option[String],
    isDefault: Boolean,
    show: Boolean
  ): Unit =
    val track = HTMLTrackElement()
    track.kind = "subtitles"
    track.src = url
    track.srclang = language.getOrElse("en")
    track.label = label.getOrElse(language.getOrElse("Subtitles"))
    if isDefault then track.setAttribute("default", "")
    element.appendChild(track)
    if show then track.track.mode = "showing"
    bump()

  // HTML5 <video> has no standard audio-track selection.
  override def audioTracks: List[AudioTrackInfo] = Nil
  override def currentAudioId: Option[String] = None
  override def audioTracksStream: Stream[Unit] = subCtrl.stream
  override def selectAudioTrack(id: String): Unit = ()

  override def seek(seconds: Double): Unit = element.currentTime = seconds

  private def applyVolume(): Unit =
    // <video>.volume caps at 1.0; boost would need WebAudio (taints cross-origin), so clamp.
    val v = (volumeLevel * gainLevel).clamp(0.0, 1.0)
    element.volume = v
    element.muted = v <= 0 // muted is what satisfies the autoplay policy

  override def setVolume(v: Double): Unit = { volumeLevel = v; applyVolume() }
  override def setGain(g: Double): Unit = { gainLevel = g; applyVolume() }
  override def setRate(v: Double): Unit = element.playbackRate = v
  override def setLooping(v: Boolean): Unit = element.loop = v
  override def setCover(v: Boolean): Unit = element.style.objectFit = if v then "cover" else "contain"
  override def setAuthHeader(v: Option[String]): Unit = authHeaderVal = v
  override def setPreferredSubtitleId(id: Option[String]): Unit = () // restored via track default
  override def setAudioFocus(v: Boolean): Unit = ()  // the browser has no audio-focus concept
  override def setAudioOnly(v: Boolean): Unit = ()   // no PCM tap on <video>
  override def audioBands: Option[Stream[List[Double]]] = Option.empty

  override def play(): Unit = element.play() // fire-and-forget; autoplay rejection is benign
  override def pause(): Unit = element.pause()

  override def position: Double = element.currentTime
  override def ready: Boolean = duration > 0 || position > 0

  override def videoSize: Option[VideoSize] =
    val w = element.videoWidth
    val h = element.videoHeight
    if w > 0 && h > 0 then Option(VideoSize(w, h)) else Option.empty

  override def duration: Double =
    val d = element.duration
    if d.isFinite then d else 0.0

  override def paused: Boolean = element.paused
  override def ended: Boolean = element.ended

  override def dispose(): Unit =
    element.pause()
    hls.foreach(h => h.destroy())
    element.removeAttribute("src")
    element.load()
    if !subCtrl.isClosed then subCtrl.close()
    ref.el = None // release our reference so the <video> + decode buffers can be GC'd

  override def view(): Widget = HtmlElementView(viewType = viewType)

object WebVideo:
  private var seq: Int = 0
  private var scriptRequested: Boolean = false

  def create(): WebVideo =
    seq += 1
    val viewType = s"sart-video-$seq"
    val el = HTMLVideoElement()
    el.controls = false // the app renders its own controls
    el.autoplay = true
    el.style.width = "100%"
    el.style.height = "100%"
    el.style.backgroundColor = "black"
    el.style.pointerEvents = "none" // let clicks pass to the Flutter overlay above
    val ref = ElementRef(Some(el))
    UiWeb.platformViewRegistry.registerViewFactory(
      viewType,
      (_: Int) => ref.el.getOrElse(HTMLVideoElement())
    )
    WebVideo(viewType, el, ref)

  /** Load hls.js (self-hosted at `/hls.min.js`) on demand; returns true once
   *  `window.Hls` is available. */
  private def ensureHls(): Future[Boolean] =
    if hlsGlobal.isDefined then Future.successful(true)
    else
      if !scriptRequested then
        scriptRequested = true
        val script = HTMLScriptElement()
        script.src = "/hls.min.js"
        script.async = true
        WebGlobals.document.head.foreach(h => h.appendChild(script))
      var i = 0
      while i < 100 && hlsGlobal.isEmpty do
        await(Futures.delayed(sart.stdlib.Duration(milliseconds = 100), () => ()))
        i += 1
      Future.successful(hlsGlobal.isDefined)
