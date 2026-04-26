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

class FxListOps:
  def takeFew(xs: List[Int]): List[Int]      = xs.take(3)
  def dropFew(xs: List[Int]): List[Int]      = xs.drop(2)
  def takeWhilePos(xs: List[Int]): List[Int] = xs.takeWhile(_ > 0)
  def dropWhilePos(xs: List[Int]): List[Int] = xs.dropWhile(_ > 0)
  def hasOdd(xs: List[Int]): Boolean         = xs.exists(_ % 2 == 1)
  def allPos(xs: List[Int]): Boolean         = xs.forall(_ > 0)
  def hasZero(xs: List[Int]): Boolean        = xs.contains(0)
  def firstZero(xs: List[Int]): Int          = xs.indexOf(0)
  def initial(xs: List[Int]): List[Int]      = xs.init
  def rest(xs: List[Int]): List[Int]         = xs.tail

class FxMapOps:
  def lookup(m: Map[String, Int], k: String): Option[Int] = m.get(k)
  def lookupOr(m: Map[String, Int], k: String): Int       = m.getOrElse(k, 0)
  def keyCount(m: Map[String, Int]): Int                  = m.size
  def hasAny(m: Map[String, Int]): Boolean                = m.nonEmpty
  def keysOf(m: Map[String, Int]): Iterable[String]       = m.keys
  def valuesOf(m: Map[String, Int]): Iterable[Int]        = m.values

class FxOptionOps:
  def orDefault(o: Option[Int], d: Option[Int]): Option[Int] = o.orElse(d)
  def hasMatch(o: Option[Int], x: Int): Boolean              = o.contains(x)
  def hasOddOpt(o: Option[Int]): Boolean                     = o.exists(_ % 2 == 1)
  def allPosOpt(o: Option[Int]): Boolean                     = o.forall(_ > 0)
  def keepEven(o: Option[Int]): Option[Int]                  = o.filter(_ % 2 == 0)

class FxSetOps:
  def setSize(s: Set[Int]): Int       = s.size
  def setHasAny(s: Set[Int]): Boolean = s.nonEmpty

class FxIterableOps:
  def firstOdd(xs: List[Int]): Option[Int] = xs.find(_ % 2 == 1)
  def positives(xs: List[Int]): Int        = xs.count(_ > 0)

class FxStringOps:
  def parseInt(s: String): Int       = s.toInt
  def parseDouble(s: String): Double = s.toDouble
  def stripped(s: String): String    = s.stripMargin

class FxListStructural:
  def flat(xss: List[List[Int]]): List[Int] = xss.flatten
  def uniq(xs: List[Int]): List[Int]        = xs.distinct
  def asc(xs: List[Int]): List[Int]         = xs.sorted
  def byLen(xs: List[String]): List[String] = xs.sortBy(_.length)
  def rfold(xs: List[Int], z: Int): Int     = xs.foldRight(z)((a, acc) => a - acc)

class FxTuples:
  def pair(a: Int, b: String): (Int, String)         = (a, b)
  def triple(a: Int, b: String, c: Int): (Int, String, Int) = (a, b, c)
  def fst(t: (Int, String)): Int                     = t._1
  def snd(t: (Int, String)): String                  = t._2
  def third(t: (Int, String, Int)): Int              = t._3

class FxPairing:
  def zipped(xs: List[Int], ys: List[String]): List[(Int, String)] = xs.zip(ys)
  def indexed(xs: List[String]): List[(String, Int)]               = xs.zipWithIndex
  def split(xs: List[Int]): (List[Int], List[Int])                 = xs.partition(_ > 0)

class FxMapMutators:
  def add(m: Map[String, Int], k: String, v: Int): Map[String, Int] = m.updated(k, v)

class FxNumericOps:
  def total(xs: List[Int]): Int    = xs.sum
  def product(xs: List[Int]): Int  = xs.product
  def smallest(xs: List[Int]): Int = xs.min
  def largest(xs: List[Int]): Int  = xs.max
