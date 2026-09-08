package sart.player

import sart.dart.*
import sart.player.web.WebVideo

/** web backend selector (hls.js `<video>`). Emitted into
 *  `platform/video_web.dart`; the conditional export routes web builds here. */
@DartLibrary("platform/video_web.dart")
@DartName("VideoBackendFactory")
object VideoBackendFactoryWeb:
  def create(): VideoPlayer = WebVideo.create()
