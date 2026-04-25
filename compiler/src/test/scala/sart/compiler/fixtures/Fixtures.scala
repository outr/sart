package sart.compiler.fixtures

// Each fixture exercises one Scala construct so the EmitterSuite can
// assert on the surrounding Dart shape. Names are intentionally
// distinctive so substring matches don't collide.

case class FxPoint(x: Int, y: Int)

class FxCounter:
  def increment(n: Int): Int = n + 1

sealed trait FxShape
class FxCircle(val radius: Double) extends FxShape
class FxSquare(val side: Double)   extends FxShape

class FxPatternMatch:
  def label(n: Int): String = n match
    case 0 => "zero"
    case 1 => "one"
    case _ => "other"

class FxGeneric[T](val value: T):
  def unwrap(): T = value

extension (s: String)
  def fxShout: String = s.toUpperCase

class FxStringConcat:
  def render(n: Int): String = "value=" + n

class FxForComp:
  def squares(xs: List[Int]): List[Int] =
    for x <- xs yield x * x
