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
 *  One remaining simplification: sideloaded subtitles are attached as a
 *  `<track src>` directly rather than fetched + SRT→VTT-converted into a
 *  `blob:` URL (that path needs List→JSArray interop; follow-up).
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

  private[web] def fireEnded(): Unit = onEndedCb.foreach(cb => cb())
  private[web] def fireError(): Unit = onErrorCb.foreach(cb => cb())
  private[web] def bump(): Unit = if !subCtrl.isClosed then subCtrl.add(())

  override def setOnEnded(cb: () => Unit): Unit = onEndedCb = Some(cb)
  override def setOnError(cb: () => Unit): Unit = onErrorCb = Some(cb)

  override def rendersImageSubtitles: Boolean = false

  override def setSource(
    url: String,
    startSeconds: Double = 0.0,
    sideloaded: List[SubtitleSource] = Nil
  ): Future[Unit] =
    if startSeconds > 0.5 then
      // Resume: set currentTime once metadata is in (a browser ignores it before).
      element.addEventListener(
        "loadedmetadata",
        ((_: Event) => element.currentTime = startSeconds).toJS
      )
    if sideloaded.nonEmpty then await(addSubtitles(sideloaded))
    val isHls = url.contains(".m3u8")
    val header = authHeaderVal
    // Force hls.js (MSE) whenever an Authorization header is required: a native
    // <video src> can play HLS on Safari but can never attach a custom header.
    val nativeHls = element.canPlayType("application/vnd.apple.mpegurl").nonEmpty
    if isHls && (header.isDefined || !nativeHls) then
      if await(WebVideo.ensureHls()) then
        hls.foreach(h => h.destroy())
        val cfg = header.map(h => hlsConfigWithAuth(h)).getOrElse(JSObject())
        val h = Hls(cfg)
        h.loadSource(url)
        h.attachMedia(element)
        hls = Some(h)
        return Future.successful(())
      else if header.isDefined then
        WebGlobals.console.error("[sart-player] hls.js failed to load".toJS)
        fireError()
        return Future.successful(())
    element.src = url
    Future.successful(())

  /** Attach each sideloaded subtitle as a `<track>` pointing at its URL. */
  override def addSubtitles(subs: List[SubtitleSource]): Future[Unit] =
    subs.foreach(s => attachTrack(s.url, s.label, s.language, s.isDefault, show = false))
    Future.successful(())

  override def subtitleTracks: List[SubtitleTrackInfo] =
    val tt = element.textTracks
    var out: List[SubtitleTrackInfo] = Nil
    var i = 0
    while i < tt.length do
      val t = tt(i)
      if t.kind == "subtitles" || t.kind == "captions" then
        val label =
          if t.label.nonEmpty then t.label
          else if t.language.nonEmpty then t.language.toUpperCase
          else "Subtitles"
        val lang = if t.language.isEmpty then Option.empty else Option(t.language)
        out = out ++ List(
          SubtitleTrackInfo(id = i.toString, label = label, language = lang, codec = Option.empty)
        )
      i += 1
    out

  override def currentSubtitleId: Option[String] =
    val tt = element.textTracks
    var found: Option[String] = None
    var i = 0
    while i < tt.length do
      if tt(i).mode == "showing" then found = Option(i.toString)
      i += 1
    found

  override def subtitleTracksStream: Stream[Unit] = subCtrl.stream

  override def selectEmbeddedSubtitle(id: String): Unit =
    val idx = id.toIntOption.getOrElse(-1)
    val tt = element.textTracks
    var i = 0
    while i < tt.length do
      tt(i).mode = if i == idx then "showing" else "disabled"
      i += 1
    bump()

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

  override def subtitlesOff(): Unit =
    val tt = element.textTracks
    var i = 0
    while i < tt.length do
      tt(i).mode = "disabled"
      i += 1
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

  /** An hls.js config whose `xhrSetup` adds the Authorization header to every
   *  request (Emby/Jellyfin HLS remux carries the token in the header). */
  private def hlsConfigWithAuth(header: String): JSObject =
    val cfg = JSObject()
    cfg.setProperty(
      "xhrSetup".toJS,
      ((xhr: JSObject, url: JSAny) =>
        val rs = xhr.getProperty("readyState".toJS)
        if rs.asInstanceOf[JSNumber].toDartInt == 0 then
          xhr.callMethod("open".toJS, "GET".toJS, url)
        xhr.callMethod("setRequestHeader".toJS, "Authorization".toJS, header.toJS)
      ).toJS
    )
    cfg

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
    val v = WebVideo(viewType, el, ref)
    el.addEventListener("ended", ((_: Event) => v.fireEnded()).toJS)
    el.addEventListener("error", ((_: Event) => v.fireError()).toJS)
    el.textTracks.addEventListener("addtrack", ((_: Event) => v.bump()).toJS)
    el.textTracks.addEventListener("removetrack", ((_: Event) => v.bump()).toJS)
    el.textTracks.addEventListener("change", ((_: Event) => v.bump()).toJS)
    v

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
