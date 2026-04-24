package sart.stdlib

import sart.dart.*

// Thin facade over `dart:async` `Stream`. Scala's `Stream`/`LazyList`
// has richer semantics (potentially-lazy sequence, finite or infinite);
// the Dart class is specifically an async event source. This facade
// targets the async shape because that's what real Flutter apps need
// (event sources for `StreamBuilder`, `listen`, etc.).

@native
@DartImport("dart:async")
class Stream[T] extends DartObject:
  def listen(onData: T => Unit): StreamSubscription[T] = native.value
  def map[R](f: T => R): Stream[R]                      = native.value
  def where(test: T => Boolean): Stream[T]              = native.value
  def asBroadcastStream: Stream[T]                       = native.value

@native
@DartImport("dart:async")
object Stream:
  def value[T](v: T): Stream[T]               = native.value
  def fromIterable[T](xs: List[T]): Stream[T] = native.value

@native
@DartImport("dart:async")
class StreamSubscription[T] extends DartObject:
  def cancel(): Unit = native.value
  def pause(): Unit  = native.value
  def resume(): Unit = native.value

@native
@DartImport("dart:async")
class StreamController[T] extends DartObject:
  def stream: Stream[T]       = native.value
  def add(value: T): Unit     = native.value
  def close(): Unit           = native.value
  def isClosed: Boolean       = native.value

@native
@DartImport("dart:async")
object StreamController:
  def broadcast[T](): StreamController[T] = native.value
