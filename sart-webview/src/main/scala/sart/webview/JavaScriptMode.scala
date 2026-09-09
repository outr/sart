package sart.webview

import sart.dart.*

/** Whether a [[WebViewController]] runs page JavaScript (`webview_flutter`).
 *  `unrestricted` is what an embedded app page (a module, a YouTube IFrame)
 *  needs; `disabled` locks it down. */
@native
@DartImport("package:webview_flutter/webview_flutter.dart")
@DartPackage("webview_flutter", "^4.14.0")
class JavaScriptMode extends DartObject

@native
@DartImport("package:webview_flutter/webview_flutter.dart")
@DartPackage("webview_flutter", "^4.14.0")
object JavaScriptMode:
  val disabled: JavaScriptMode = native.value
  val unrestricted: JavaScriptMode = native.value
