package sart.player.web

import sart.dart.*

/** A `<video>` element (package:web). The web backend's playback surface. */
@native
@DartImport("package:web/web.dart")
@DartPackage("web", "^1.1.0")
class HTMLVideoElement() extends Node:
  var controls: Boolean = native.value
  var autoplay: Boolean = native.value
  var loop: Boolean = native.value
  var muted: Boolean = native.value
  var src: String = native.value
  var currentTime: Double = native.value
  var volume: Double = native.value
  var playbackRate: Double = native.value
  def paused: Boolean = native.value
  def ended: Boolean = native.value
  def duration: Double = native.value
  def videoWidth: Int = native.value
  def videoHeight: Int = native.value
  def textTracks: TextTrackList = native.value
  def style: CSSStyleDeclaration = native.value
  def canPlayType(mimeType: String): String = native.value
  def addEventListener(eventType: String, listener: JSFunction): Unit = native.value
  def play(): JSPromise[JSAny] = native.value
  def pause(): Unit = native.value
  def removeAttribute(name: String): Unit = native.value
  def load(): Unit = native.value
