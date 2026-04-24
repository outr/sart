package example.features

import sart.stdlib.{Try, Success, Failure, Either, Left, Right}

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

class EitherExampleSart:
  def okRight(x: Int): Either[String, Int] = Right(x)
  def okLeft(s: String): Either[String, Int] = Left(s)

  def bimapRight(e: Either[String, Int]): Either[String, String] =
    e.map(n => n.toString)

  def extract(e: Either[String, Int]): String =
    e.fold(err => "error: " + err, n => "ok: " + n.toString)
