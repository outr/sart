package sart.web

import sart.dart.*

/** The `localStorage` global (`@native` facade). Named lowercase to match the
 *  browser global; resolves to the host's own `localStorage`. */
@native
object localStorage:
  def getItem(key: String): String = native.value
  def setItem(key: String, value: String): Unit = native.value
