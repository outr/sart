package sart.stdlib

import sart.dart.*

// Dart-native members of `num`/`int`/`double`/`String` that Scala's own
// types don't carry. `@native` extension defs emit nothing — call sites
// emit plain member calls that resolve against the real Dart types.

extension (d: Double)
  @native def toStringAsFixed(fractionDigits: Int): String = native.value
  @native def clamp(lowerLimit: Double, upperLimit: Double): Double = native.value

extension (i: Int)
  @native def toRadixString(radix: Int): String = native.value
  @native def clamp(lowerLimit: Int, upperLimit: Int): Int = native.value

extension (s: String)
  @native def padLeft(width: Int, padding: String): String = native.value
  @native def padRight(width: Int, padding: String): String = native.value
  @native def codeUnitAt(index: Int): Int = native.value
  @native def codeUnits: List[Int] = native.value
