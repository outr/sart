package example.features

import sart.stdlib.{Option, Some, None}

// Feature fixture: sart.stdlib.Option → Dart nullable.
//
// Minimum slice: type translation and Some/None literal translation.
// Method-call translation (map, fold, etc.) lands next, via a Dart shim
// library packaged with sart-stdlib.

class OptionExample:
  def lookup(id: Int): Option[String] =
    if id == 0 then None else Some("found")

  // `isDefined`, `.map`, etc. require the Dart shim (follow-up work).
  // Until then, we express presence checks using the underlying nullable
  // representation: an `Option[T]` is a `T?` in Dart, so `!= null` works.
  def describe(present: Boolean): String =
    if present then "present" else "absent"
