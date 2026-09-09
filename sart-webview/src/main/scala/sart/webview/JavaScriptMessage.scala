package sart.webview

import sart.dart.*

/** A message posted from page JavaScript to the host over a channel registered
 *  with [[WebViewController.addJavaScriptChannel]] — `message` is the string
 *  the page sent. */
@native
@DartImport("package:webview_flutter/webview_flutter.dart")
@DartPackage("webview_flutter", "^4.14.0")
class JavaScriptMessage extends DartObject:
  def message: String = native.value
