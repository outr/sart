package sart.player.mediakit

import sart.dart.*
import flutter.material.{Widget, BoxFit, Key}

/** The video render widget for a [[VideoController]] (media_kit_video).
 *  `controls` takes a controls-builder — pass [[MediaKitControls.NoVideoControls]]
 *  to render none. */
@native
@DartImport("package:media_kit_video/media_kit_video.dart")
@DartPackage("media_kit_video", "^2.0.1")
class Video(
  val controller: VideoController,
  val controls: DartObject = native.value,
  val fit: BoxFit = native.value,
  val key: Key = native.value
) extends Widget
