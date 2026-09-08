package sart.player

import sart.dart.*
import sart.stdlib.{Duration, Stream}
import sart.stdlib.clamp
import sart.player.mediakit.{
  Player, PlayerConfiguration, MPVLogLevel, Media, VideoController, PlaylistMode,
  Video, MediaKitControls, SubtitleTrack => MkSubtitleTrack, AudioTrack => MkAudioTrack
}
import flutter.material.{Widget, BoxFit}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** iOS / desktop / Android: media_kit (libmpv). libmpv does its own
 *  demux/decode and renders into a Flutter texture, so playback (incl. HEVC)
 *  isn't hostage to the platform's surface path — and, unlike video_player,
 *  it fills the box correctly on Android TV. Faithful port of NaboPlayer's
 *  `MediaKitVideo`, Nabo-specific coupling removed. Emitted into its own
 *  `video_io.dart` library so its `media_kit` imports never reach the web bundle. */
@DartLibrary("platform/video_io.dart")
class MediaKitVideo extends VideoPlayer:
  private val player: Player =
    Player(configuration = PlayerConfiguration(logLevel = MPVLogLevel.info))
  private var controller: VideoController = null
  private var onEndedCb: Option[() => Unit] = None
  private var onErrorCb: Option[() => Unit] = None
  private var endedFired: Boolean = false
  private var disposed: Boolean = false
  private var coverFit: Boolean = false
  private var volumeLevel: Double = 1.0
  private var gainLevel: Double = 1.0

  // Constructor body (runs after field initialisers).
  controller = VideoController(player)
  player.stream.completed.listen(done =>
    if done && !endedFired then
      endedFired = true
      onEndedCb.foreach(cb => cb())
  )
  player.stream.error.listen(_ => onErrorCb.foreach(cb => cb()))

  /** Run a player command unless disposed — media_kit asserts on commands
   *  issued after teardown, so a disposed player is left alone. */
  private def cmd(op: () => Future[Unit]): Unit =
    if !disposed then op()

  override def setOnEnded(cb: () => Unit): Unit = onEndedCb = Some(cb)
  override def setOnError(cb: () => Unit): Unit = onErrorCb = Some(cb)

  override def videoSize: Option[VideoSize] =
    val w = player.state.width.getOrElse(0)
    val h = player.state.height.getOrElse(0)
    if w > 0 && h > 0 then Option(VideoSize(w, h)) else Option.empty

  override def rendersImageSubtitles: Boolean = false

  override def setSource(
    url: String,
    startSeconds: Double = 0.0,
    sideloaded: List[SubtitleSource] = Nil
  ): Future[Unit] =
    if disposed then Future.successful(())
    else
      endedFired = false // re-arm per source so playlist rotation gets an onEnded
      try
        if startSeconds > 0.5 then
          // Resume: open paused, wait until ready (duration known) so the seek
          // isn't dropped, seek, then play.
          await(player.open(Media(url), play = false))
          await(seekWhenReady(startSeconds))
          await(player.play())
        else await(player.open(Media(url)))
        if sideloaded.nonEmpty then await(addSubtitles(sideloaded))
      catch case _: Throwable => if !disposed then () else ()
      Future.successful(())

  private def seekWhenReady(seconds: Double): Future[Unit] =
    if player.state.duration.inMilliseconds <= 0 then
      await(player.stream.duration.firstWhere(d => d.inMilliseconds > 0))
    await(player.seek(Duration(milliseconds = (seconds * 1000).round.toInt)))
    Future.successful(())

  override def addSubtitles(subs: List[SubtitleSource]): Future[Unit] =
    // Apply only the server-selected default; with none, leave subtitles OFF.
    val defaults = subs.filter(s => s.isDefault)
    if defaults.isEmpty then Future.successful(())
    else
      val pick = defaults.head
      try
        await(player.setSubtitleTrack(
          MkSubtitleTrack.uri(pick.url, title = pick.label.orNull, language = pick.language.orNull)
        ))
      catch case _: Throwable => ()
      Future.successful(())

  override def subtitleTracks: List[SubtitleTrackInfo] =
    player.state.tracks.subtitle
      .filter(t => t.id != "no" && t.id != "auto")
      .map(t =>
        SubtitleTrackInfo(
          id = t.id,
          label = t.title.getOrElse(t.language.map(l => l.toUpperCase).getOrElse("Subtitles")),
          language = t.language,
          codec = t.codec
        )
      )

  override def currentSubtitleId: Option[String] =
    val s = player.state.track.subtitle
    if s.id == "no" || s.id == "auto" then Option.empty else Option(s.id)

  override def subtitleTracksStream: Stream[Unit] = player.stream.tracks.map(_ => ())

  override def selectEmbeddedSubtitle(id: String): Unit =
    val t = player.state.tracks.subtitle.find(s => s.id == id).getOrElse(MkSubtitleTrack.no())
    cmd(() => player.setSubtitleTrack(t))

  override def selectUriSubtitle(
    url: String,
    label: Option[String] = Option.empty,
    language: Option[String] = Option.empty
  ): Unit =
    cmd(() => player.setSubtitleTrack(MkSubtitleTrack.uri(url, title = label.orNull, language = language.orNull)))

  override def subtitlesOff(): Unit = cmd(() => player.setSubtitleTrack(MkSubtitleTrack.no()))

  override def audioTracks: List[AudioTrackInfo] =
    player.state.tracks.audio
      .filter(t => t.id != "no" && t.id != "auto")
      .map(t =>
        AudioTrackInfo(
          id = t.id,
          label = t.title.getOrElse(t.language.map(l => l.toUpperCase).getOrElse("Audio")),
          language = t.language
        )
      )

  override def currentAudioId: Option[String] =
    val a = player.state.track.audio
    if a.id == "no" || a.id == "auto" then Option.empty else Option(a.id)

  override def audioTracksStream: Stream[Unit] = player.stream.tracks.map(_ => ())

  override def selectAudioTrack(id: String): Unit =
    val t = player.state.tracks.audio.find(a => a.id == id).getOrElse(MkAudioTrack.auto())
    cmd(() => player.setAudioTrack(t))

  override def seek(seconds: Double): Unit =
    cmd(() => player.seek(Duration(milliseconds = (seconds * 1000).round.toInt)))

  private def applyVolume(): Unit =
    cmd(() => player.setVolume((volumeLevel * gainLevel * 100).clamp(0, 200)))

  override def setVolume(v: Double): Unit = { volumeLevel = v; applyVolume() }
  override def setGain(g: Double): Unit = { gainLevel = g; applyVolume() }
  override def setLooping(v: Boolean): Unit =
    cmd(() => player.setPlaylistMode(if v then PlaylistMode.loop else PlaylistMode.none))
  override def setCover(v: Boolean): Unit = coverFit = v
  override def setPreferredSubtitleId(id: Option[String]): Unit = ()
  override def setAuthHeader(v: Option[String]): Unit = ()
  override def setAudioFocus(v: Boolean): Unit = ()
  override def setAudioOnly(v: Boolean): Unit = ()
  override def audioBands: Option[Stream[List[Double]]] = Option.empty

  override def play(): Unit = cmd(() => player.play())
  override def pause(): Unit = cmd(() => player.pause())
  override def setRate(v: Double): Unit = cmd(() => player.setRate(v))

  override def position: Double = player.state.position.inMilliseconds / 1000.0
  override def duration: Double = player.state.duration.inMilliseconds / 1000.0
  override def paused: Boolean = !player.state.playing
  override def ended: Boolean =
    val dms = player.state.duration.inMilliseconds
    dms > 0 && player.state.position.inMilliseconds >= dms

  override def dispose(): Unit =
    if !disposed then
      disposed = true
      player.dispose()

  override def view(): Widget =
    Video(
      controller = controller,
      controls = MediaKitControls.NoVideoControls,
      fit = if coverFit then BoxFit.cover else BoxFit.contain
    )
