package sart.stdlib

import sart.dart.*
import scala.concurrent.Future

/** `dart:async` Future extension methods that Scala's `Future` lacks. */
extension [T](f: Future[T])
  /** Complete with [onTimeout]'s value if [f] hasn't completed within
   *  [duration] — Dart's `Future.timeout(duration, onTimeout: …)`. */
  @native def timeout(duration: Duration, onTimeout: () => T): Future[T] = native.value
