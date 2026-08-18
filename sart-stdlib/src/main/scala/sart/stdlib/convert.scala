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
  def base64Encode(bytes: List[Int]): String = native.value
  def base64Encode(bytes: Uint8List): String = native.value

/** `dart:convert`'s configurable JSON encoder (`withIndent` for
 *  pretty-printed exports).
 */
@native
@DartImport("dart:convert")
class JsonEncoder extends DartObject:
  def convert(obj: Any): String = native.value

@native
@DartImport("dart:convert")
object JsonEncoder:
  def withIndent(indent: String): JsonEncoder = native.value

/** `dart:convert`'s utf8 codec object. */
@native
@DartImport("dart:convert")
object utf8:
  def encode(input: String): List[Int] = native.value
  def decode(codeUnits: List[Int]): String = native.value
  def decode(bytes: Uint8List): String = native.value
