package example.features

// Feature fixture: pattern matching → Dart 3 switch expressions.

class PatternMatchExample:
  def label(n: Int): String =
    n match
      case 0 => "zero"
      case 1 => "one"
      case _ => "other"

  def classifyEnum(f: Filter): String =
    f match
      case Filter.All       => "all"
      case Filter.Active    => "active"
      case Filter.Completed => "completed"

  def positiveOnly(n: Int): String =
    n match
      case x if x > 0 => "positive"
      case 0          => "zero"
      case _          => "negative"

  def typeClassify(x: Any): String =
    x match
      case s: String => "string"
      case _: Int    => "int"
      case _         => "other"
