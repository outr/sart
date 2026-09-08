package sart.player.mediakit

import sart.dart.*
import sart.stdlib.{Duration, Stream}

/** A [[Player]]'s reactive state as `dart:async` streams. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class PlayerStream extends DartObject:
  def completed: Stream[Boolean] = native.value
  def error: Stream[String] = native.value
  def duration: Stream[Duration] = native.value
  def position: Stream[Duration] = native.value
  def tracks: Stream[Tracks] = native.value
