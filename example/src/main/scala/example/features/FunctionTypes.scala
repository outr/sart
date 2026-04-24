package example.features

// Feature fixture: Scala FunctionN types used as field/param declarations
// (not just as inferred lambda values). Each should land as a Dart
// `R Function(T1, …, Tn)` function type.

class Callback(val fn: () => Unit):
  def invoke(): Unit = fn()

class BinaryOp(val op: (Int, Int) => Int):
  def apply(a: Int, b: Int): Int = op(a, b)

class Mapper[A, B](val f: A => B):
  def run(a: A): B = f(a)
