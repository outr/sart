package example.features

// Feature fixture: sealed trait + case class hierarchy (ADT).
// Expected Dart: sealed base class with concrete subclasses.

sealed trait Shape
case class Circle(radius: Double) extends Shape
case class Rectangle(width: Double, height: Double) extends Shape
case object UnitSquare extends Shape

// Sealed abstract class variant.
sealed abstract class Json
case class JsonString(value: String) extends Json
case class JsonNumber(value: Double) extends Json
case class JsonBool(value: Boolean) extends Json
case object JsonNull extends Json
