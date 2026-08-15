package sart.stdlib

import sart.dart.*

/** `dart:convert` top-level JSON codecs. `jsonDecode` hands back [[Dyn]]
 *  — Sart's face of Dart `dynamic` — for string-keyed traversal of wire
 *  payloads (`json("user")("name").str`).
 */
@native
@DartImport("dart:convert")
@DartTopLevel
object convert:
  def jsonEncode(value: Any): String  = native.value
  def jsonDecode(source: String): Dyn = native.value
