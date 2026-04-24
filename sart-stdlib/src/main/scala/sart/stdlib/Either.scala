package sart.stdlib

import sart.dart.*

// `Either[L, R]` — hand-ported sealed hierarchy, right-biased like Scala's.
// Emitter ships `sart_either.dart` alongside `main.dart` when referenced.

@native
@DartImport("sart_either.dart")
sealed abstract class Either[L, R]:
  def map[R2](f: R => R2): Either[L, R2]             = native.value
  def flatMap[R2](f: R => Either[L, R2]): Either[L, R2] = native.value
  def fold[X](onLeft: L => X, onRight: R => X): X    = native.value
  def isLeft: Boolean                                 = native.value
  def isRight: Boolean                                = native.value

// Case classes so Scala pattern matching gets auto-`unapply` for
// `case Left(v) => …` / `case Right(v) => …` — the apply-in-companion
// approach still disambiguates against `scala.Left`/`scala.Right`.
@native
@DartImport("sart_either.dart")
case class Left[L, R](value: L) extends Either[L, R]

@native
@DartImport("sart_either.dart")
case class Right[L, R](value: R) extends Either[L, R]
