package example.features

// Feature fixture: Scala range constructors compile to Dart's
// `List<int>.generate`, with the result materialising as a real List
// so subsequent `.map`/`.where`/`.toList` calls go through the
// existing list-like rewrites.

class RangeExample:
  // Inclusive: 1..n  →  [1, 2, …, n]
  def inclusive(n: Int): List[Int] = (1 to n).toList
  // Exclusive: 0..<n →  [0, 1, …, n-1]
  def exclusive(n: Int): List[Int] = (0 until n).toList
  // Chained: ranges chain into the list-like rewrite for free.
  def squared(n: Int): List[Int] = (0 until n).map(i => i * i).toList
