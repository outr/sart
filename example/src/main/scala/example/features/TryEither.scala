package example.features

import sart.stdlib.{Try, Success, Failure, Either, Left, Right, Option}

// Feature fixture: Sart's Try + Either stdlib facades.
//
// Each resolves to a real Dart sealed hierarchy defined in a shim library
// that the emitter drops alongside main.dart. `.map`/`.flatMap`/`.fold`
// dispatch to that shim at runtime.

class TryExampleSart:
  def ok(x: Int): Try[Int]  = Success(x)
  def bad(e: Throwable): Try[Int] = Failure(e)

  def doubled(t: Try[Int]): Try[Int] = t.map(n => n * 2)
  def value(t: Try[Int], default: Int): Int = t.getOrElse(default)
  // Bridges Try to Sart's Option (a Dart nullable). Success → value, Failure → null.
  def maybe(t: Try[Int]): Option[Int] = t.toOption

class EitherExampleSart:
  def okRight(x: Int): Either[String, Int] = Right(x)
  def okLeft(s: String): Either[String, Int] = Left(s)

  def bimapRight(e: Either[String, Int]): Either[String, String] =
    e.map(n => n.toString)

  def extract(e: Either[String, Int]): String =
    e.fold(err => "error: " + err, n => "ok: " + n.toString)

  // Right-biased getOrElse: returns the right value if present, else default.
  def rightOr(e: Either[String, Int], default: Int): Int = e.getOrElse(default)
  // Right value as nullable Option (Sart maps Option to Dart's T?).
  def maybeRight(e: Either[String, Int]): Option[Int] = e.toOption
  // Flip Left/Right: errors become "successes" for piping into right-biased ops.
  def flip(e: Either[String, Int]): Either[Int, String] = e.swap
