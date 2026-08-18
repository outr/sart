package sart.stdlib

import sart.dart.*

/** dart:typed_data's Uint8List — the byte-payload currency (http bodies,
 *  screenshots, file save/upload). It IS a `List<int>` in Dart; expose
 *  just the members the ports touch.
 */
@native
@DartImport("dart:typed_data")
class Uint8List extends DartObject:
  def length: Int = native.value
