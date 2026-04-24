package sart.stdlib

import sart.dart.*

// `dart:core`'s `RegExp`. Scala spells this `Regex` via
// `scala.util.matching.Regex`; we keep the Scala-style name Scala-side
// and use `@DartName` to rewrite to `RegExp` on the Dart side.

@native
@DartName("RegExp")
class Regex(val pattern: String):
  def hasMatch(input: String): Boolean = native.value
  def firstMatch(input: String): Match = native.value

@native
@DartName("RegExpMatch")
class Match:
  def group(groupIdx: Int): String = native.value
