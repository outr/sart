package sart.webview

import sart.dart.*

/** Page-lifecycle callbacks for a [[WebViewController]] (`webview_flutter`),
 *  set via [[WebViewController.setNavigationDelegate]]. Each callback is
 *  optional. `onWebResourceError` receives a `WebResourceError`, typed loosely
 *  here since the port only surfaces it; add a typed facade if you need its
 *  fields.
 */
@native
@DartImport("package:webview_flutter/webview_flutter.dart")
@DartPackage("webview_flutter", "^4.14.0")
class NavigationDelegate(
  onPageStarted: String => Unit = native.value,
  onPageFinished: String => Unit = native.value,
  onProgress: Int => Unit = native.value,
  onWebResourceError: Any => Unit = native.value
) extends DartObject
