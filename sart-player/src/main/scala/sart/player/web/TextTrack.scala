package sart.player.web

import sart.dart.*

/** A `<video>` text track (subtitles/captions). `mode` is `"showing"` /
 *  `"disabled"` / `"hidden"`. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class TextTrack extends DartObject:
  def kind: String = native.value
  def label: String = native.value
  def language: String = native.value
  var mode: String = native.value
