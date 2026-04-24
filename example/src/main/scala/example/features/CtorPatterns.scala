package example.features

import sart.stdlib.{Try, Success, Failure, Either, Left, Right}

// Fixture: constructor patterns in match — core 1.0 feature for
// destructuring ADTs. Hits case classes, Try/Success/Failure, and
// Either/Left/Right.

case class Vec(x: Int, y: Int)

class CtorPatternsExample:
  def describe(p: Vec): String =
    p match
      case Vec(0, 0) => "origin"
      case Vec(0, _) => "on y-axis"
      case Vec(_, 0) => "on x-axis"
      case Vec(a, b) => "(" + a.toString + ", " + b.toString + ")"

  def recover(t: Try[Int]): Int =
    t match
      case Success(v) => v
      case Failure(_) => -1

  def eitherToString(e: Either[String, Int]): String =
    e match
      case Left(err)  => "err: " + err
      case Right(v)   => "ok: " + v.toString
