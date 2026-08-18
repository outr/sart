package sart.dart

/** The Scala face of Dart's `dynamic` — the universal JSON/wire type
 *  (`Map<String, dynamic>` values, `jsonDecode` results, transport
 *  payloads). Scala's `Any` can't play this role: it lowers to Dart
 *  `Object?`, which forbids member access.
 *
 *  The emitter gives these members dedicated lowerings:
 *    - `d(key)`   → `d[key]`          (index access)
 *    - `d(key) = v` → `d[key] = v`    (index assignment)
 *    - `d.str`    → `(d as String)`
 *    - `d.toInt`  → `(d as int)`
 *    - `d.toBool` → `(d as bool)`
 *    - `d.isNull` → `(d == null)`
 *    - `d.toList` → `(d as List<dynamic>)`
 */
@native
class Dyn extends DartObject:
  def apply(key: String): Dyn = native.value
  def apply(index: Int): Dyn  = native.value
  def update(key: String, value: Any): Unit = native.value
  def str: String             = native.value
  def toInt: Int              = native.value
  def toBool: Boolean         = native.value
  def isNull: Boolean         = native.value
  def toList: List[Dyn]       = native.value
