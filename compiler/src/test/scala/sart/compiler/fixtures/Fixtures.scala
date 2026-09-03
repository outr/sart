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

class FxRanges:
  def inclusive(n: Int): List[Int] = (1 to n).toList
  def exclusive(n: Int): List[Int] = (0 until n).toList
  def squared(n: Int): List[Int]   = (0 until n).map(i => i * i).toList

import sart.dart.{native, DartImport, DartLibrary, DartName, DartVariants, Dyn, Json, JsonField, JsonModel, JsonTag}


@JsonModel
case class FxKeyed(@JsonField("_id") id: String, label: String)

class FxScalaOpt:
  def find(id: Int): Option[String] = if id == 0 then None else Some("found")

class FxLifecycleBase:
  def initState(): Unit = ()
  def register(): Unit = ()

class FxLifecycle extends FxLifecycleBase:
  override def initState(): Unit =
    super.initState()
    register()

class FxForeach:
  def visitAll(xs: List[String], f: String => Unit): Unit = xs.foreach(f)
  def join(xs: List[String], ys: List[String]): List[String] = xs ++ ys

@sart.dart.native
@sart.dart.DartImport("dart:math")
@sart.dart.DartAlias("fxmath")
@sart.dart.DartTopLevel
object FxMathAliased:
  def sqrt(x: Double): Double = sart.dart.native.value

class FxAliasUse:
  def root(x: Double): Double = FxMathAliased.sqrt(x)

class FxJsonBridge:
  def parse(d: Dyn): FxUser = Json.decode[FxUser](d)
  def render(u: FxUser): Dyn = Json.encode(u)

@JsonModel
case class FxUser(name: String, age: Int, tags: List[String], nickname: Option[String])

@JsonModel
sealed trait FxKind
@JsonTag("Kind.Basic")
case class FxKindBasic() extends FxKind
@JsonTag("Kind.Rich")
case class FxKindRich(level: Int) extends FxKind

@JsonModel
sealed trait FxWire
@JsonTag("Wire.Ping")
case class FxPing(seq: Int) extends FxWire
case class FxPong(seq: Int, note: String) extends FxWire

object FxRegistry:
  var current: String = ""
  val limit: Int = 10
  def isActive: Boolean = current != ""
  def register(name: String): String =
    current = name
    name

class FxBase(val label: String):
  def describe(): String = "base " + label

class FxDerived(label: String, val extra: Int) extends FxBase(label):
  override def describe(): String = "derived " + extra.toString

trait FxClickable:
  def click(): String = "clicked"

class FxButtonish extends FxBase("btn") with FxClickable

class FxNamedParams:
  def greet(name: String, punct: String = "!", times: Int = 1): String =
    name + punct + times.toString
  def callAll(): String  = greet("a")
  def callSome(): String = greet("a", times = 3)
  def callPos(): String  = greet("a", ".")

case class FxStyled(label: String, size: Int = 12, bold: Boolean = false)

class FxNamedCtor:
  def make(): FxStyled    = FxStyled("x")
  def makeBig(): FxStyled = FxStyled("x", bold = true)
  def resize(s: FxStyled): FxStyled = s.copy(size = 20)

class FxFutureCtor:
  import scala.concurrent.Future
  def ready(n: Int): Future[Int]          = Future.successful(n)
  def broken(e: Exception): Future[Int]   = Future.failed(e)

class FxWhileLoop:
  def countdown(n: Int): Int =
    var i = n
    var total = 0
    while i > 0 do
      total = total + i
      i = i - 1
    total

class FxLazyInit:
  lazy val expensive: Int = 6 * 7
  def local(n: Int): Int =
    lazy val doubled = n * 2
    doubled + 1

class FxAsync:
  import scala.concurrent.Future
  import sart.dart.{async, await}
  def fetch(): Future[Int] = Future.successful(1)
  def load(): Future[Int] = async {
    val n = await(fetch())
    n + 1
  }
  def fire(): Unit =
    val x = await(fetch())
    var sink = x
    sink = sink + 1

class FxOptionMatch:
  def render(o: Option[String]): String = o match
    case Some(v) => v
    case None    => "none"

extension (s: String) def fxRepeat(n: Int): String = s + n.toString

class FxExtUse:
  def go(s: String): String = s.fxRepeat(2)

trait FxMixinOn extends FxCounter:
  def label(): String = "m"

class FxWithMixinOn extends FxCounter with FxMixinOn


// ── Annotation-free wire models (shared-module shape: no Sart references) ──

case class FxPlainModel(id: String, count: Int, tags: List[String], note: Option[String])

sealed trait FxGeom
object FxGeom:
  case class Circle(radius: Double) extends FxGeom
  case class Box(w: Int, h: Int) extends FxGeom
  // The JVM side's codec — no Dart form; must be skipped, loudly.
  implicit val rw: fabric.rw.RW[Circle] = { import fabric.rw.*; RW.gen }

class FxGeomUse:
  def area(s: FxGeom): Double = s match
    case FxGeom.Circle(r) => 3.0 * r * r
    case FxGeom.Box(w, h) => (w * h).toDouble
  def circle(r: Double): FxGeom = FxGeom.Circle(r)
  def decode(d: Dyn): FxGeom = Json.decode[FxGeom](d)

// A case class holding a widget-ish function field is NOT wire-shaped —
// no codec must be synthesized for it.
case class FxNotWire(label: String, onTap: () => Unit)

// fabric's `Json` value type rides as `dynamic`; its builders lower to
// Dart literals.
case class FxApiRecord(id: String, extra: fabric.Json, tags: List[fabric.Json])
object FxApiConsts:
  val Private: fabric.Json = fabric.obj("type" -> fabric.str("SourceType.Private"))
  val Empty: fabric.Json = fabric.obj()
  val Nums: fabric.Json = fabric.arr(fabric.num(1), fabric.bool(true))

// ── Enumerations: fabric `RW.enumeration` style case objects ──────────────

sealed trait FxColor
object FxColor:
  case object Red extends FxColor
  case object Blue extends FxColor
  val values: List[FxColor] = List(Red, Blue)
  def parse(s: String): FxColor = if s == "red" then Red else Blue
  implicit val rw: fabric.rw.RW[FxColor] = { import fabric.rw.*; RW.enumeration(List(Red, Blue)) }

case class FxPaint(name: String, color: FxColor, alt: Option[FxColor])

class FxColorUse:
  def isRed(c: FxColor): Boolean = c == FxColor.Red
  def label(c: FxColor): String = c match
    case FxColor.Red => "red"
    case FxColor.Blue => "blue"
  def pick(): FxColor = FxColor.Blue
  def roundTrip(d: Dyn): FxPaint = Json.decode[FxPaint](d)

// A mixed hierarchy: the case object is a const singleton, not an enum.
sealed trait FxToken
object FxToken:
  case object EOF extends FxToken
  case class Word(text: String) extends FxToken

class FxTokenUse:
  def eof(): FxToken = FxToken.EOF
  def isEof(t: FxToken): Boolean = t match
    case FxToken.EOF => true
    case FxToken.Word(_) => false

// ── Types Dart can't express: erased to `dynamic` (see emitTypeRef) ──────

trait FxGraph:
  type Node
  def nodes: List[Node]
  def first: Node = nodes.head

class FxIntGraph extends FxGraph:
  type Node = Int
  def nodes: List[Int] = List(1, 2)

class FxBox[T](val value: T):
  def map[U](f: T => U): FxBox[U] = FxBox(f(value))

object FxTypes:
  def firstOf(g: FxGraph): g.Node = g.first
  def roundTrip(g: FxGraph): Boolean = g.nodes.contains(firstOf(g))
  def either(x: Int | String): String = x.toString
  def both(x: FxGraph & FxBox[Int]): Int = x.value
  def refined(x: FxGraph { type Node = Int }): Int = x.first + 1
  def same(g: FxGraph)(h: g.type): Boolean = g == h
  opaque type UserId = String
  object UserId:
    def apply(s: String): UserId = s
  def show(u: UserId): String = u
  trait Functor[F[_]]:
    def fmap[A, B](fa: F[A])(f: A => B): F[B]
  def lift[F[_], A](fn: Functor[F], fa: F[A], f: A => A): F[A] = fn.fmap(fa)(f)
  given boxFunctor: Functor[FxBox] with
    def fmap[A, B](fa: FxBox[A])(f: A => B): FxBox[B] = fa.map(f)
  def liftBox(b: FxBox[Int]): FxBox[Int] = lift(boxFunctor, b, (x: Int) => x + 1)
  type Elem[X] = X match
    case List[t] => t
    case String  => Char
  def head(xs: List[Int]): Elem[List[Int]] = xs.head

// A case object under a parent WITH ctor params: no const chain is
// possible, so the object becomes a private-ctor singleton factory.
abstract class FxSoloBase(val n: Int)
case object FxSolo extends FxSoloBase(1)
class FxSoloUse:
  def solo(): FxSoloBase = FxSolo


// Class-body statements are the Scala constructor body → Dart ctor `{ … }`.
class FxCtorBody:
  var n: Int = 0
  bump()
  def bump(): Unit = n += 1

class FxCtorBodyParams(val a: Int):
  var total: Int = 0
  total = a * 2


// `Option(x)` / `Option.empty` are Dart nullability identities.
class FxOptionApply:
  def wrap(s: String): Option[String] = Option(s)
  def none(): Option[Int] = Option.empty[Int]


// ── Platform-variant emission: switch facade + two variant libraries. ──
@native
@DartImport("platform/fx_fs.dart")
@DartVariants(default = "platform/fx_fs_stub.dart", web = "platform/fx_fs_web.dart")
object FxFs:
  def isFullscreen(): Boolean = native.value
  def label(o: Option[String]): String = native.value

@DartLibrary("platform/fx_fs_stub.dart")
@DartName("FxFs")
object FxFsStub:
  def isFullscreen(): Boolean = false
  def label(o: Option[String]): String = o.getOrElse("none")

@DartLibrary("platform/fx_fs_web.dart")
@DartName("FxFs")
object FxFsWeb:
  def isFullscreen(): Boolean = true
  def label(o: Option[String]): String = o.getOrElse("web")

class FxFsUse:
  def check(): Boolean = FxFs.isFullscreen()


// ── Per-member enum wire values: @JsonTag overrides the default
// "Parent.Member" convention (nabo's @JsonValue('movie') equivalent). ──
sealed trait FxReviewLevel
object FxReviewLevel:
  @JsonTag("movie") case object Movie extends FxReviewLevel
  @JsonTag("episode") case object Episode extends FxReviewLevel
  case object Other extends FxReviewLevel

enum FxQuality:
  @JsonTag("sd") case Low
  @JsonTag("hd") case High
