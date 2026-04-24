package example.features

// Feature fixture: given / using. Scala 3 resolves implicits at typer time
// so TASTy sees explicit argument lists; the Sart emitter's existing
// curry-flattener handles the shape. Top-level givens become Dart finals.

trait Formatter[T]:
  def format(value: T): String

given intFormatter: Formatter[Int] with
  def format(value: Int): String = "int=" + value

class GivenExample:
  def greet[T](value: T)(using f: Formatter[T]): String =
    "hello " + f.format(value)

  def demo: String = greet(42)
