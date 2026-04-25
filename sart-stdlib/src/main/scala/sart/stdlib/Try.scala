package sart.stdlib

import sart.dart.*

// `Try[T]` — hand-ported sealed hierarchy for Scala's `Try` without a
// built-in Dart analog. The Sart compiler ships `sart_try.dart` alongside
// `main.dart` whenever `Try` appears; the Dart shim defines the real
// classes (with `map`/`flatMap`/`fold`/`get` semantics) that the Scala
// facades below reference via `@native`.

@native
@DartImport("sart_try.dart")
sealed abstract class Try[T]:
  def map[R](f: T => R): Try[R]                                   = native.value
  def flatMap[R](f: T => Try[R]): Try[R]                          = native.value
  def fold[R](onFailure: Throwable => R, onSuccess: T => R): R    = native.value
  def getOrElse[U >: T](default: U): U                            = native.value
  def isSuccess: Boolean                                           = native.value
  def isFailure: Boolean                                           = native.value
  // Sart maps `Option[T]` to a Dart nullable `T?`, so `toOption`
  // returns the value on Success and `null` on Failure.
  def toOption: Option[T]                                          = native.value

// `case class` subtypes give us the Scala `unapply` for pattern
// matching (`case Success(v) => …`) while staying @native on the Dart
// side — the Dart shim defines the real class.
@native
@DartImport("sart_try.dart")
case class Success[T](value: T) extends Try[T]

@native
@DartImport("sart_try.dart")
case class Failure[T](error: Throwable) extends Try[T]
