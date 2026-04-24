package example.features

// Feature fixture: Map and Set literals, Scala → Dart collection literals.
//
// Translations:
//   Map("a" -> 1, "b" -> 2)  →  {'a': 1, 'b': 2}
//   Set(1, 2, 3)             →  {1, 2, 3}

class MapSetExample:
  def ages: Map[String, Int] = Map("alice" -> 30, "bob" -> 25)
  def primes: Set[Int] = Set(2, 3, 5, 7, 11)

  def contains(m: Map[String, Int], key: String): Boolean = m.contains(key)
