package sart.web

import sart.dart.*

/** The browser `location` global (`@native` facade). Only the URL `hash` is
 *  needed — the launcher uses it to deep-link a demo (e.g. `#dice`). Named
 *  lowercase to match the JS global; `location` is a browser alias for
 *  `window.location`. */
@native
object location:
  def hash: String = native.value
