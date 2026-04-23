package sart.dart

/** Structural root for Dart-side types. Facade classes typically extend this
 *  (transitively) so Scala typechecking keeps them distinct from pure-Scala
 *  values.
 *
 *  The emitter treats this as a no-op — references to `DartObject` in
 *  `extends` clauses of facades are dropped, just like `js.Object` is in
 *  Scala.js output.
 */
@native
abstract class DartObject

/** Marker for types that correspond to Dart's built-in `Function` type. For
 *  the counter app we only need a zero-arg void callback, but keeping this
 *  as a separate hierarchy lets us grow the facade vocabulary later.
 */
@native
abstract class DartFunction extends DartObject
