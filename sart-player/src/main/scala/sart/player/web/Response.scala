package sart.player.web

import sart.dart.*

/** A fetch `Response` (package:web). `text()` resolves the body as a string
 *  (always UTF-8 decoded), which is where SRT→VTT conversion begins. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class Response extends JSObject:
  def text(): JSPromise[JSString] = native.value
