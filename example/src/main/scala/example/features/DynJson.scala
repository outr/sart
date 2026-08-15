package example.features

import sart.dart.Dyn
import sart.stdlib.convert.*

// Feature fixture: the `Dyn` dynamic bridge + dart:convert JSON codecs.
//
// The Sart emitter:
//   - maps `sart.dart.Dyn` → Dart `dynamic` in type position
//   - lowers `d(key)` → `d[key]`, `d.str` → `(d as String)`,
//     `d.toInt` → `(d as int)`, `d.isNull` → `(d == null)`
//   - emits `jsonDecode` / `jsonEncode` as dart:convert top-level calls
//
// This is the wire-boundary idiom both reference apps live on
// (`Map<String, dynamic>` payloads with string-tag dispatch).

class DynJson:
  def nameOf(payload: String): String =
    val json = jsonDecode(payload)
    if json("name").isNull then "unknown" else json("name").str

  def ageOf(payload: String): Int =
    jsonDecode(payload)("age").toInt

  def render(name: String, age: Int): String =
    jsonEncode(Map("name" -> name, "age" -> age))
