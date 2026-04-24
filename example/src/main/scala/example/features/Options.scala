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

  // Native-operator translations: these map to Dart's `??`, `!= null`,
  // `== null` at emit time — no shim library needed.
  def valueOrZero(o: Option[Int]): Int     = o.getOrElse(0)
  def isPresent(o: Option[String]): Boolean = o.isDefined
  def isAbsent(o: Option[String]): Boolean  = o.isEmpty

  // Shim-backed method translations. Each of these dispatches to the
  // Dart extension in `lib/sart_option.dart` that the emitter drops
  // alongside main.dart whenever Option appears in the output.
  def addOne(o: Option[Int]): Option[Int]    = o.map(x => x + 1)
  def chain(o: Option[Int]): Option[Int]     = o.flatMap(x => Some(x * 2))
  def describe(o: Option[Int]): String       = o.fold("nothing")(x => "got " + x.toString)
