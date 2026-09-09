package sart.web

import sart.dart.*

/** A DOM element (`@native` facade — the emitter emits calls to it verbatim as
 *  plain JS; nothing here is bundled). Render-on-change: build a tree with
 *  `document.createElement`, set attributes, append children/text, and mount
 *  it under a container. */
@native
class Element extends DartObject:
  def appendChild(child: Element): Element = native.value
  def setAttribute(name: String, value: String): Unit = native.value
  def addEventListener(event: String, handler: KeyEvent => Unit): Unit = native.value
