package example.features

// Feature fixture: try/catch/finally structural translation.
// We deliberately exercise the *shape* of try/catch/finally, not Scala's
// stdlib exceptions — `FormatException`/`NumberFormatException` mapping is
// a Phase 2 stdlib concern, so the catches here use facade-friendly names.

import flutter.material.BuildContext

class TryExample:

  def fetchOrDefault(lookup: Int, fallback: Int): Int =
    try
      lookup + 1
    catch
      case _: Exception => fallback

  def failing(): Int = throw new Exception("boom")

  def recoverBound(): String =
    try
      "ok"
    catch
      case e: Exception =>
        "failed"
    finally
      ()
