package sart.web

import sart.dart.*

/** A callback-shaped XMLHttpRequest helper (`@native` facade). Deliberately
 *  callback-based — matching the old-engine floor (webOS 3 / Tizen 2.3: no
 *  fetch, unreliable Promises) — so no Future→callback lowering is needed.
 *  `Xhr` resolves to a small host-provided helper (a few lines in the page),
 *  never a bundled library. */
@native
object Xhr:
  def get(url: String, onOk: String => Unit, onErr: () => Unit): Unit = native.value
