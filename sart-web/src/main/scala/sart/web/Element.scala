package sart.web

import sart.dart.*

/** A DOM element (`@native` facade — the emitter emits calls to it verbatim as
 *  plain JS; nothing here is bundled). Render-on-change: build a tree with
 *  `document.createElement`, set text/attributes, append children, and mount it
 *  under a container. The settable `var`s are real DOM properties (assigning
 *  one emits `el.prop = v`); `innerHTML = ""` empties a node. */
@native
class Element extends DartObject:
  var textContent: String = native.value
  var className: String = native.value
  var value: String = native.value
  var innerHTML: String = native.value
  def appendChild(child: Element): Element = native.value
  def setAttribute(name: String, value: String): Unit = native.value
  def addEventListener(event: String, handler: KeyEvent => Unit): Unit = native.value
