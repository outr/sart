package sart.player.web

import sart.dart.*

/** The `URL` interface (package:web). `createObjectURL` mints a `blob:` URL
 *  for a [[Blob]], which a `<track src>` can then point at. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
object URL:
  def createObjectURL(obj: JSObject): String = native.value
