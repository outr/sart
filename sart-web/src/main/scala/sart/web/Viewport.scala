package sart.web

import sart.dart.*

/** Scroll utilities (`@native` facade over the host runtime). `centerById`
 *  scrolls the element with the given id into view within its scroll container
 *  — centered horizontally, no vertical jump — for keeping the D-pad-focused
 *  item visible on a TV rail. */
@native
object Viewport:
  def centerById(id: String): Unit = native.value
