package sart.player

import sart.dart.*

/** The platform switch: Sart generates `platform/video_backend.dart` as a
 *  conditional export over the io / web variant libraries, so each build
 *  gets exactly one backend and the other's imports (media_kit vs.
 *  package:web / dart:ui_web) never leak in. */
@native
@DartImport("platform/video_backend.dart")
@DartVariants(
  default = "platform/video_io.dart",
  web = "platform/video_web.dart"
)
object VideoBackendFactory:
  def create(): VideoPlayer = native.value
