package example.features

// Feature fixture: Scala 3 extension methods → Dart `extension` blocks.

extension (n: Int)
  def squared: Int = n * n
  def plusOne: Int = n + 1

extension (s: String)
  def repeatN(count: Int): String = s * count
