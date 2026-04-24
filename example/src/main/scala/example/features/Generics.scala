package example.features

// Feature fixture: generics on user classes and methods.
// Translated Dart carries type parameters on class and method signatures.
//
// (The `::` and `.size` forms from Scala's `List` are left for the stdlib
// shim work in Phase 2 — using them here would mix two concerns.)

class Box[T](val value: T):
  def get(): T = value

case class Pair[A, B](first: A, second: B)

// Generic method demonstrating `[T]` on a DefDef.
class Wrapping:
  def wrap[T](value: T): Box[T] = Box(value)

// Bounded generics — Scala `[T <: Bound]` → Dart `<T extends Bound>`.
trait HasSize:
  def size: Int

class BoundedBox[T <: HasSize](val inner: T):
  def measure: Int = inner.size
