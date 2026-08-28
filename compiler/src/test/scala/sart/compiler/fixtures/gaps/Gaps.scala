package sart.compiler.fixtures.gaps

// Constructs the emitter deliberately does not translate. Inspected by a
// separate emitter run in EmitterSuite so the main fixtures keep their
// "no TODO markers" invariant. Strict mode reports each at its Scala
// source location.
class FxGap:
  def outer(x: Int): Int =
    def inner(y: Int): Int = y + 1
    inner(x)
