package sart.player

import sart.dart.*
import sart.player.mediakit.MediaKit

/** io backend selector (media_kit). Emitted into `platform/video_io.dart`;
 *  the conditional export routes non-web builds here. */
@DartLibrary("platform/video_io.dart")
@DartName("VideoBackendFactory")
object VideoBackendFactoryIo:
  def create(): VideoPlayer =
    MediaKit.ensureInitialized() // load libmpv before the first Player (idempotent)
    MediaKitVideo()
