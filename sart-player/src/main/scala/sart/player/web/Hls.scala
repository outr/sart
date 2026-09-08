package sart.player.web

import sart.dart.*

/** hls.js interop (loaded on demand for HLS on browsers without native HLS).
 *  A `@JS('Hls')` extension type; use is guarded so a load/interop failure
 *  falls back to the native `<video>` src. */
@JsType("Hls")
@DartLibrary("platform/video_web.dart")
class Hls(config: JSObject) extends JSObject:
  def loadSource(url: String): Unit = native.value
  def attachMedia(video: HTMLVideoElement): Unit = native.value
  def destroy(): Unit = native.value

/** `window.Hls` — null until the hls.js script has loaded. */
@JsGlobal("window.Hls")
@DartLibrary("platform/video_web.dart")
def hlsGlobal: Option[JSAny] = native.value
