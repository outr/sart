package sart.dart

import scala.concurrent.Future

/** dart:js_interop value types and conversions, for authoring browser-JS
 *  bindings in Scala (see @JsType / @JsGlobal). Every type here maps to
 *  the same-named dart:js_interop type; the emitter adds the import.
 *
 *  These bodies never run on the JVM — a Sart app runs as Dart.
 */

@native @DartImport("dart:js_interop")
trait JSAny extends DartObject
@native @DartImport("dart:js_interop")
trait JSObject extends JSAny:
  // js_interop_unsafe dynamic member access.
  def getProperty(name: JSAny): JSAny = native.value
  def setProperty(name: JSAny, value: JSAny): Unit = native.value
  def callMethod(name: JSAny, args: JSAny*): JSAny = native.value
@native @DartImport("dart:js_interop")
object JSObject:
  /** `JSObject()` — a fresh JS object. */
  def apply(): JSObject = native.value

@native @DartImport("dart:js_interop")
trait JSString extends JSAny
@native @DartImport("dart:js_interop")
trait JSNumber extends JSAny:
  def toDartInt: Int = native.value
  def toDartDouble: Double = native.value
@native @DartImport("dart:js_interop")
trait JSBoolean extends JSAny
@native @DartImport("dart:js_interop")
trait JSFunction extends JSAny
@native @DartImport("dart:js_interop")
trait JSArray[T <: JSAny] extends JSAny
@native @DartImport("dart:js_interop")
trait JSPromise[T <: JSAny] extends JSAny

/** Dart→JS and JS→Dart conversions. `x.toJS` / `p.toDart` emit verbatim
 *  (the emitter passes the `.toJS`/`.toDart` member through). Provided as
 *  extensions so any value carries them, as in real js_interop.
 */
extension (s: String)  @native def toJS: JSString = native.value
extension (n: Int)     @native def toJS: JSNumber = native.value
extension (d: Double)  @native def toJS: JSNumber = native.value
extension (b: Boolean) @native def toJS: JSBoolean = native.value
extension [T](f: T)    @native def toJS: JSFunction = native.value
extension (a: JSAny)   @native def dartify(): Any = native.value
extension [T <: JSAny](p: JSPromise[T]) @native def toDart: Future[T] = native.value
