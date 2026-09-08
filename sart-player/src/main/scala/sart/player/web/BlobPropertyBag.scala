package sart.player.web

import sart.dart.*

/** Options for a [[Blob]] (package:web) — carries the MIME `type`, so a
 *  subtitle blob is tagged `text/vtt`. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class BlobPropertyBag(`type`: String) extends JSObject
