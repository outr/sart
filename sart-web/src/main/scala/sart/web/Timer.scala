package sart.web

import sart.dart.*

/** A periodic timer (`@native` facade). `Timer.periodic(ms, cb)` runs `cb`
 *  every `ms` milliseconds (host `setInterval`); `cancel()` stops it
 *  (`clearInterval`). Used for per-second UI ticks (the stopwatch demo). */
@native
class Timer extends DartObject:
  def cancel(): Unit = native.value

@native
object Timer:
  def periodic(ms: Int, callback: () => Unit): Timer = native.value
