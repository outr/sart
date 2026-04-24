package sart.stdlib

import sart.dart.*

// `dart:math` facade. Methods on this `object` correspond to top-level
// Dart functions in `dart:math`, so the emitter drops the `math.` prefix
// at call sites (see `@DartTopLevel`).

@native
@DartImport("dart:math")
@DartTopLevel
object math:
  // Trig
  def sin(x: Double): Double  = native.value
  def cos(x: Double): Double  = native.value
  def tan(x: Double): Double  = native.value
  def asin(x: Double): Double = native.value
  def acos(x: Double): Double = native.value
  def atan(x: Double): Double = native.value
  def atan2(y: Double, x: Double): Double = native.value

  // Roots / powers / logs
  def sqrt(x: Double): Double = native.value
  def pow(x: Double, y: Double): Double = native.value
  def exp(x: Double): Double  = native.value
  def log(x: Double): Double  = native.value

  // Min/max (Dart has min/max for num — we expose the Double overload
  // because Scala's `scala.math` does the same for simplicity).
  def min(a: Double, b: Double): Double = native.value
  def max(a: Double, b: Double): Double = native.value

  // Constants
  val pi: Double = native.value
  val e: Double  = native.value

// `Random` from `dart:math`. The Scala `scala.util.Random` vocabulary
// is close but not identical; this facade maps Dart's shape directly.

@native
@DartImport("dart:math")
class Random:
  def nextInt(max: Int): Int    = native.value
  def nextDouble(): Double       = native.value
  def nextBool(): Boolean        = native.value
