package sart.player.mediakit

import sart.dart.*

/** A subtitle track known to libmpv (embedded, or one attached by URI).
 *  `id` `"no"`/`"auto"` are the sentinel off/automatic tracks. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
class SubtitleTrack extends DartObject:
  def id: String = native.value
  def title: Option[String] = native.value
  def language: Option[String] = native.value
  def codec: Option[String] = native.value

@native
@DartImport("package:media_kit/media_kit.dart")
object SubtitleTrack:
  def uri(
    uri: String,
    title: String = native.value,
    language: String = native.value
  ): SubtitleTrack = native.value
  def no(): SubtitleTrack = native.value
  def auto(): SubtitleTrack = native.value
