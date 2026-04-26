package example.features

// Feature fixture: Scala tuples emit as Dart records.
//   - `(Int, String)` type ↔ Dart `(int, String)` record type
//   - `(a, b)` literal     ↔ Dart `(a, b)` record literal
//   - `t._1` / `t._2`      ↔ Dart `t.$1` / `t.$2` positional getters
//
// Requires the emitted pubspec's SDK floor to be Dart ≥ 3.0 (records);
// Sart targets ≥ 3.11 by default, so the analyzer accepts these.

class TupleExample:
  def pair(a: Int, b: String): (Int, String) = (a, b)
  def first(t: (Int, String)): Int           = t._1
  def second(t: (Int, String)): String       = t._2
  def swap(t: (Int, String)): (String, Int)  = (t._2, t._1)

  // Tuples-via-collection-ops: zip / zipWithIndex / partition.
  def zipped(xs: List[Int], ys: List[String]): List[(Int, String)] = xs.zip(ys)
  def indexed(xs: List[String]): List[(String, Int)]               = xs.zipWithIndex
  def split(xs: List[Int]): (List[Int], List[Int])                 = xs.partition(_ > 0)

  // Map immutable update via spread.
  def addEntry(m: Map[String, Int], k: String, v: Int): Map[String, Int] =
    m.updated(k, v)
