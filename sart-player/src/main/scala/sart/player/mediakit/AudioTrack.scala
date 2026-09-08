package sart.player.mediakit

import sart.dart.*

/** An audio track known to libmpv. `id` `"no"`/`"auto"` are the sentinel
 *  off/automatic tracks. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class AudioTrack extends DartObject:
  def id: String = native.value
  def title: Option[String] = native.value
  def language: Option[String] = native.value

@native
@DartImport("package:media_kit/media_kit.dart")
object AudioTrack:
  def auto(): AudioTrack = native.value
  def no(): AudioTrack = native.value
