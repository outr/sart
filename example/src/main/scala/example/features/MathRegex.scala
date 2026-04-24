package example.features

import sart.stdlib.{math, Random, Regex}

// Feature fixture: dart:math-backed stdlib (math + Random) and the
// `Regex` → Dart `RegExp` mapping.

class MathRegexExample:
  def hypot(a: Double, b: Double): Double =
    math.sqrt(math.pow(a, 2.0) + math.pow(b, 2.0))

  def clampAngle(radians: Double): Double = math.atan2(math.sin(radians), math.cos(radians))

  def quarterCircle: Double = math.pi / 4.0

  def dice(r: Random): Int = r.nextInt(6) + 1

  def matchesNumber(s: String): Boolean =
    Regex("^\\d+$").hasMatch(s)
