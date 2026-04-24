package example.features

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// Feature fixture: Scala Future → Dart Future.
//
// The Sart emitter:
//   - maps scala.concurrent.Future[T] → Dart Future<T>
//   - translates `.map(f)` / `.flatMap(f)` to Dart `.then(f)`
//   - drops the implicit ExecutionContext argument the Scala typer inserts
//
// User code can keep writing idiomatic Scala Future chains; the Dart
// output is a first-class async pipeline.

class FutureExample:
  def chain(f: Future[Int]): Future[Int] = f.map(x => x + 1)

  def compose(a: Future[Int], b: Future[Int]): Future[Int] =
    a.flatMap(x => b.map(y => x + y))
