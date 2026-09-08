package sart.tv.media

import sart.dart.*
import sart.stdlib.{Duration, Uri}
import scala.concurrent.Future

// Facades for the `audio_service` package (0.18.x) — OS-level media
// session: the lock-screen / notification / Bluetooth-remote / CarPlay /
// Android-Auto "now playing" surface and its transport controls. These
// mirror the Dart API faithfully; `sart.tv.media.MediaSession` wraps them
// with a Scala-friendly layer most apps use instead.
//
// Every facade class carries @DartPackage so the dependency lands in the
// emitted pubspec as soon as the app references any of them.

/** rxdart's `BehaviorSubject` — `audio_service` exposes its state streams
 *  as these; only `.add` (push a new value) is needed to drive the OS UI.
 *  The type name never appears in emitted Dart (callers only `.add` on an
 *  inherited field), so it rides the audio_service import and needs no
 *  direct rxdart dependency — rxdart comes in transitively. */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
class BehaviorSubject[T] extends DartObject:
  def add(value: T): Unit = native.value
  def value: T = native.value

/** How far along loading/playback is — drives the OS spinner/among states. */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
class AudioProcessingState extends DartObject

@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
object AudioProcessingState:
  def idle: AudioProcessingState = native.value
  def loading: AudioProcessingState = native.value
  def buffering: AudioProcessingState = native.value
  def ready: AudioProcessingState = native.value
  def completed: AudioProcessingState = native.value
  def error: AudioProcessingState = native.value

/** A transport action the OS may surface or deliver. */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
class MediaAction extends DartObject

@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
object MediaAction:
  def stop: MediaAction = native.value
  def pause: MediaAction = native.value
  def play: MediaAction = native.value
  def rewind: MediaAction = native.value
  def skipToPrevious: MediaAction = native.value
  def skipToNext: MediaAction = native.value
  def fastForward: MediaAction = native.value
  def playPause: MediaAction = native.value
  def seek: MediaAction = native.value

/** A button shown on the notification / control centre. */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
class MediaControl(
  val androidIcon: String,
  val label: String,
  val action: MediaAction
) extends DartObject

@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
object MediaControl:
  def play: MediaControl = native.value
  def pause: MediaControl = native.value
  def stop: MediaControl = native.value
  def rewind: MediaControl = native.value
  def fastForward: MediaControl = native.value
  def skipToNext: MediaControl = native.value
  def skipToPrevious: MediaControl = native.value

/** Now-playing metadata: what the OS shows on the lock screen / control
 *  centre. `id` and `title` are required; the rest are optional. */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
class MediaItem(
  val id: String,
  val title: String,
  val album: String = native.value,
  val artist: String = native.value,
  val genre: String = native.value,
  val duration: Duration = native.value,
  val artUri: Uri = native.value,
  val playable: Boolean = native.value,
  val displayTitle: String = native.value,
  val displaySubtitle: String = native.value,
  val displayDescription: String = native.value,
  val isLive: Boolean = native.value
) extends DartObject

/** Current transport state: what's playing, where, and which controls the
 *  OS should offer. */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
class PlaybackState(
  val processingState: AudioProcessingState = native.value,
  val playing: Boolean = native.value,
  val controls: List[MediaControl] = native.value,
  val systemActions: Set[MediaAction] = native.value,
  val androidCompactActionIndices: List[Int] = native.value,
  val updatePosition: Duration = native.value,
  val bufferedPosition: Duration = native.value,
  val speed: Double = native.value,
  val queueIndex: Int = native.value
) extends DartObject:
  def copyWith(
    processingState: AudioProcessingState = native.value,
    playing: Boolean = native.value,
    controls: List[MediaControl] = native.value,
    systemActions: Set[MediaAction] = native.value,
    androidCompactActionIndices: List[Int] = native.value,
    updatePosition: Duration = native.value,
    bufferedPosition: Duration = native.value,
    speed: Double = native.value,
    queueIndex: Int = native.value
  ): PlaybackState = native.value

/** Marker supertype for the handler `AudioService.init` returns. */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
abstract class AudioHandler extends DartObject

/** The subclassable base handler. Extend it, override the transport
 *  methods the OS calls, and push state through `playbackState` /
 *  `mediaItem`. [[sart.tv.media.SartAudioHandler]] is a ready-made
 *  subclass. */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
abstract class BaseAudioHandler extends AudioHandler:
  val playbackState: BehaviorSubject[PlaybackState] = native.value
  val mediaItem: BehaviorSubject[MediaItem] = native.value
  val queue: BehaviorSubject[List[MediaItem]] = native.value

  def play(): Future[Unit] = native.value
  def pause(): Future[Unit] = native.value
  def stop(): Future[Unit] = native.value
  def seek(position: Duration): Future[Unit] = native.value
  def skipToNext(): Future[Unit] = native.value
  def skipToPrevious(): Future[Unit] = native.value
  def fastForward(): Future[Unit] = native.value
  def rewind(): Future[Unit] = native.value
  def setSpeed(speed: Double): Future[Unit] = native.value

/** Platform config for the media session (notification channel, intervals). */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
class AudioServiceConfig(
  val androidNotificationChannelId: String = native.value,
  val androidNotificationChannelName: String = native.value,
  val androidNotificationOngoing: Boolean = native.value,
  val androidStopForegroundOnPause: Boolean = native.value,
  val androidNotificationIcon: String = native.value,
  val androidShowNotificationBadge: Boolean = native.value,
  val fastForwardInterval: Duration = native.value,
  val rewindInterval: Duration = native.value,
  val preloadArtwork: Boolean = native.value
) extends DartObject

/** Entry point: initialise the media session once at startup. */
@native
@DartImport("package:audio_service/audio_service.dart")
@DartPackage("audio_service", "^0.18.19")
object AudioService:
  def init[T <: AudioHandler](
    builder: () => T,
    config: AudioServiceConfig = native.value
  ): Future[T] = native.value
