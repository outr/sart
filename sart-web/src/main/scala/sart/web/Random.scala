package sart.web

import sart.dart.*

/** Random numbers (`@native` facade). `Random.nextInt(bound)` returns an int in
 *  `[0, bound)` (host `Math.floor(Math.random() * bound)`). */
@native
object Random:
  def nextInt(bound: Int): Int = native.value
