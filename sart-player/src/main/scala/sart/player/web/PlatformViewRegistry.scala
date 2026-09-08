package sart.player.web

import sart.dart.*

/** dart:ui_web's platform-view registry: maps a view-type id to a factory
 *  that builds the DOM element Flutter's `HtmlElementView` mounts. */
@native
@DartImport("dart:ui_web")
class PlatformViewRegistry extends DartObject:
  def registerViewFactory(viewType: String, factory: Int => DartObject): Unit = native.value
