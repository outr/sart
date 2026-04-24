package example.features

// Feature fixture: idiomatic Scala List operations translate to Dart
// List/Iterable equivalents without the author having to think about
// naming differences.

class ListOpsExample:
  def count(xs: List[Int]): Int       = xs.size
  def first(xs: List[Int]): Int       = xs.head
  def present(xs: List[Int]): Boolean = xs.nonEmpty
  def joined(xs: List[String]): String = xs.mkString(", ")
