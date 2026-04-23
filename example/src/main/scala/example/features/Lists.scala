package example.features

// Feature fixture: checking what Scala's built-in `List` emits today.
// The hand-ported `sart.stdlib.List` with a Dart shim is a separate follow-up
// (Phase 2); this file is purely to surface current behaviour and identify
// gaps for the next stdlib work.

class ListExample:
  def build: List[Int] = List(1, 2, 3)

  def triple(xs: List[Int]): List[Int] = xs
