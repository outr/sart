package sart.player.mediakit

import sart.dart.*

/** media_kit_video's top-level controls builders. [[NoVideoControls]] is the
 *  builder that draws nothing — used when the app renders its own controls. */
@native
@DartImport("package:media_kit_video/media_kit_video.dart")
@DartPackage("media_kit_video", "^2.0.1")
@DartTopLevel
object MediaKitControls:
  def NoVideoControls: DartObject = native.value
  def AdaptiveVideoControls: DartObject = native.value
