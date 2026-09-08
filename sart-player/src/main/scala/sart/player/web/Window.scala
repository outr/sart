package sart.player.web

import sart.dart.*

/** The browser `window` (package:web). Only `fetch` is needed here — to pull
 *  a sideloaded subtitle's bytes into JS for SRT→VTT conversion. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class Window extends DartObject:
  def fetch(input: JSAny): JSPromise[Response] = native.value
