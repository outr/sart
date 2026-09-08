package sart.player.mediakit

import sart.dart.*
import sart.stdlib.Duration

/** A snapshot of a [[Player]]'s current state. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class PlayerState extends DartObject:
  def width: Option[Int] = native.value
  def height: Option[Int] = native.value
  def duration: Duration = native.value
  def position: Duration = native.value
  def playing: Boolean = native.value
  def tracks: Tracks = native.value
  def track: Track = native.value
