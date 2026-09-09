package sart.webview

import sart.dart.*
import flutter.material.{Widget, Key}

/** The widget that renders a [[WebViewController]]'s page (`webview_flutter`).
 *  `controller` is required — configure it first, then drop this into the
 *  tree. */
@native
@DartImport("package:webview_flutter/webview_flutter.dart")
@DartPackage("webview_flutter", "^4.14.0")
class WebViewWidget(
  controller: WebViewController,
  key: Key = native.value
) extends Widget
