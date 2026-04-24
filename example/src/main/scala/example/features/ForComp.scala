package example.features

// Feature fixture: for-comprehensions. Scala 3 desugars these at TASTy
// time to a chain of `.map`/`.flatMap`/`.withFilter` calls, so success
// here depends on the underlying collection having those methods — List
// works out of the box because Dart's `List` already provides them.

class ForCompExample:
  def incremented(xs: List[Int]): List[Int] =
    for x <- xs yield x + 1

  def positives(xs: List[Int]): List[Int] =
    for
      x <- xs
      if x > 0
    yield x

  def pairsSum(xs: List[Int], ys: List[Int]): List[Int] =
    for
      x <- xs
      y <- ys
    yield x + y
