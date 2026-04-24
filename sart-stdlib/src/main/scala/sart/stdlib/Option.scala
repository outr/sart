package sart.stdlib

import sart.dart.*

// `Option[T]` — a hand-ported stdlib type mapped to Dart's `T?` nullable.
//
// At emit time, the Sart compiler applies three special cases:
//
//   - Type `sart.stdlib.Option[T]`   → Dart `T?`
//   - `Some(x)`                      → `x`       (Dart promotes to nullable)
//   - `None`                         → `null`
//
// Method calls on `Option` (`map`, `flatMap`, `fold`, `getOrElse`, …) are
// delegated to a Dart shim library `sart_option.dart` that provides them
// as extensions on nullable types. That shim is landed in a follow-up.

// `Option` is covariant in `T` so `None: Option[Nothing]` is assignable
// to any `Option[T]` — the same variance the standard library ships.
@native
abstract class Option[+T]:
  def map[R](f: T => R): Option[R]              = native.value
  def flatMap[R](f: T => Option[R]): Option[R]  = native.value
  def getOrElse[U >: T](default: U): U          = native.value
  def fold[R](ifEmpty: R)(f: T => R): R         = native.value
  def isDefined: Boolean                        = native.value
  def isEmpty: Boolean                          = native.value
  // For side-effecting operations (like `opt.foreach(t => t.cancel())`)
  // where the lambda returns Unit. Avoids the `R extends Object`
  // generic constraint on `.map` that the Dart shim uses.
  def foreach(f: T => Unit): Unit               = native.value

@native
object Some:
  def apply[T](value: T): Option[T] = native.value

// Singleton None. Extending `Option[Nothing]` (+T variance) lets this
// one value type-check everywhere any `Option[T]` is expected.
@native
object None extends Option[Nothing]
