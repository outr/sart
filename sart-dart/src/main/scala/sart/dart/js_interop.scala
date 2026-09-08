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
trait JSObject extends JSAny
@native @DartImport("dart:js_interop")
object JSObject:
  /** `JSObject()` — a fresh JS object. */
  def apply(): JSObject = native.value

/** Dynamic member access on a JS object. These are `dart:js_interop_unsafe`
 *  extension methods, so each carries that import — it's added to whichever
 *  library actually uses them, not to `dart:js_interop`. */
extension (o: JSObject)
  @native @DartImport("dart:js_interop_unsafe")
  def getProperty(name: JSAny): JSAny = native.value
  @native @DartImport("dart:js_interop_unsafe")
  def setProperty(name: JSAny, value: JSAny): Unit = native.value
  // Fixed arity (name + two args) — Dart's `callMethod` has optional
  // positional args, but Sart doesn't yet strip an EXTENSION method's
  // omitted defaults at the call site (they leak a `…$default$N` ref), so
  // the common two-arg shape is spelled explicitly.
  @native @DartImport("dart:js_interop_unsafe")
  def callMethod(name: JSAny, arg1: JSAny, arg2: JSAny): JSAny = native.value

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
