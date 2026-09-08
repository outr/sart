package sart.player.mediakit

import sart.dart.*

/** media_kit's process-wide initialiser. Must be called once before any
 *  [[Player]] is created (loads libmpv); idempotent. */
@native
@DartImport("package:media_kit/media_kit.dart")
@DartPackage("media_kit", "^1.2.6")
object MediaKit:
  def ensureInitialized(): Unit = native.value
