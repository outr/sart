package sart.web

import sart.dart.*

/** A DOM keyboard event (`@native` facade). `keyCode` carries the D-pad /
 *  remote key on TV browsers (37 left, 39 right, 38 up, 40 down, 13 enter). */
@native
class KeyEvent extends DartObject:
  def keyCode: Int = native.value
