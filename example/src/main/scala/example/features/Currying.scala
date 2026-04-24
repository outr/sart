package example.features

// Feature fixture: curried method calls (multiple Scala parameter lists)
// flatten to a single Dart argument list.

class CurryingExample:
  def total(xs: List[Int]): Int =
    xs.foldLeft(0)((acc, x) => acc + x)

  def userDefined(x: Int)(y: Int): Int = x + y

  def invoke(): Int = userDefined(3)(4)
