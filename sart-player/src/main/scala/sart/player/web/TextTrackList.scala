package sart.player.web

import sart.dart.*

/** The live list of a `<video>`'s text tracks. Indexable; fires
 *  add/remove/change events. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class TextTrackList extends DartObject:
  def length: Int = native.value
  def apply(index: Int): TextTrack = native.value
  def addEventListener(eventType: String, listener: JSFunction): Unit = native.value
