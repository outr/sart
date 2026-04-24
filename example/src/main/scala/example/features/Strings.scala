package example.features

// Feature fixture: common Scala String operations translated through
// Predef's implicit StringOps wrapper (augmentString). Each method
// call should emit as a native Dart String call.

class StringExample:
  def shout(s: String): String = s.toUpperCase
  def whisper(s: String): String = s.toLowerCase
  def trimmed(s: String): String = s.trim
  def empty(s: String): Boolean = s.isEmpty
  def parts(s: String): List[String] = s.split(",").toList
  def swap(s: String): String = s.replaceAll("a", "b")
  def len(s: String): Int = s.length
