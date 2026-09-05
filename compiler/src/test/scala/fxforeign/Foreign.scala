package fxforeign

// Mock "foreign library" types for wire-mapping tests. This package is
// OUTSIDE sart.compiler.fixtures, so the suite never walks its TASTy —
// the emitter sees these exactly like jar-provided library types.
case class FxId[T](value: String)
case class FxStamp(value: Long)
case class FxWrapped(a: String, b: Int)
trait FxSchemaBase[T]
abstract class FxDocBase[T]

sealed trait FxDir
object FxDir:
  case object Up extends FxDir
  case object Down extends FxDir
