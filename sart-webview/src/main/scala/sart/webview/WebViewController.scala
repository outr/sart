package sart.webview

import sart.dart.*
import sart.stdlib.Uri
import flutter.material.Color
import scala.concurrent.Future

/** Drives a [[WebViewWidget]] (`webview_flutter`): load pages, run JavaScript,
 *  bridge messages from the page back to Dart, and observe navigation. Create
 *  one, configure it (JavaScript mode, channels, navigation delegate), then
 *  `loadRequest` a `Uri`. Every mutating call returns a `Future` (the port
 *  usually fires them and moves on, as Dart cascades do). */
@native
@DartImport("package:webview_flutter/webview_flutter.dart")
@DartPackage("webview_flutter", "^4.14.0")
class WebViewController() extends DartObject:
  def setJavaScriptMode(mode: JavaScriptMode): Future[Unit] = native.value
  def setBackgroundColor(color: Color): Future[Unit] = native.value
  def setNavigationDelegate(delegate: NavigationDelegate): Future[Unit] = native.value
  def setUserAgent(userAgent: String): Future[Unit] = native.value
  def enableZoom(enabled: Boolean): Future[Unit] = native.value

  def loadRequest(uri: Uri): Future[Unit] = native.value
  def loadHtmlString(html: String): Future[Unit] = native.value
  def loadFlutterAsset(key: String): Future[Unit] = native.value
  def loadFile(absoluteFilePath: String): Future[Unit] = native.value

  def runJavaScript(javaScript: String): Future[Unit] = native.value
  def runJavaScriptReturningResult(javaScript: String): Future[Any] = native.value

  def addJavaScriptChannel(
    name: String,
    onMessageReceived: JavaScriptMessage => Unit
  ): Future[Unit] = native.value
  def removeJavaScriptChannel(name: String): Future[Unit] = native.value

  def goBack(): Future[Unit] = native.value
  def goForward(): Future[Unit] = native.value
  def reload(): Future[Unit] = native.value
  def clearCache(): Future[Unit] = native.value
  def clearLocalStorage(): Future[Unit] = native.value
