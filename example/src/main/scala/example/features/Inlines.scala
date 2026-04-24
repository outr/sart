package example.features

// Feature fixture: Scala 3 `inline def`. The compiler expands each call
// site at TASTy time, so the emitter should see the expanded code and
// not need special handling. Also exercises `transparent inline` where
// the compile-time signature is kept.

inline def doubled(x: Int): Int = x * 2

class InlineExample:
  def tripleWrap(n: Int): Int = doubled(n) + n
