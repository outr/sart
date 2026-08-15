package sart.dart

/** Scala-side entry points to the Dart codecs Sart synthesizes for
 *  `@JsonModel` types. The emitter lowers:
 *    - `Json.decode[T](dyn)` → `T.fromJson(dyn as Map<String, dynamic>)`
 *    - `Json.encode(model)`  → `model.toJson()`
 */
object Json:
  def decode[T](json: Dyn): T =
    throw new Error("sart.dart.Json.decode evaluated at runtime — not translated to Dart")
  def encode(value: Any): Dyn =
    throw new Error("sart.dart.Json.encode evaluated at runtime — not translated to Dart")
