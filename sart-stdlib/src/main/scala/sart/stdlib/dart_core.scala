package sart.stdlib

import sart.dart.*

// dart:core statics and types that Scala's own types can't carry —
// `String`/`Int` have no companions to hang the Dart statics on, so
// `@DartName`d objects stand in.

@native
@DartName("String")
object DartString:
  def fromCharCodes(charCodes: Uint8List): String = native.value

@native
@DartName("int")
object DartInt:
  def parse(source: String, radix: Int = native.value): Int = native.value

/** dart:core FormatException — positional message ctor. */
@native
class FormatException(message: String = native.value) extends RuntimeException
