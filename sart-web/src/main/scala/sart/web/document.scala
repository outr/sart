package sart.web

import sart.dart.*

/** The DOM `document` global (`@native` facade). Named lowercase to match the
 *  browser global exactly — the emitter emits `document.getElementById(...)`
 *  verbatim; it resolves to the host's own `document`, never bundled. */
@native
object document:
  def getElementById(id: String): Element = native.value
  def createElement(tag: String): Element = native.value
  def createTextNode(text: String): Element = native.value
  def addEventListener(event: String, handler: KeyEvent => Unit): Unit = native.value
