package sart.player.web

import sart.dart.*

/** A `Blob` (package:web) — an immutable blob of bytes. Built from an array
 *  of parts (`JSArray`) so the fetched-and-converted VTT text can be handed
 *  to `URL.createObjectURL`. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class Blob(parts: JSArray[JSAny], options: BlobPropertyBag) extends JSObject
