package sart.stdlib

import sart.dart.*

// `dart:core`'s `Duration`. Implicit import — no `@DartImport` needed
// since `dart:core` is in every Dart library's namespace by default.
@native
class Duration(
  val milliseconds: Int = native.value,
  val seconds: Int = native.value,
  val minutes: Int = native.value,
  val hours: Int = native.value,
  val days: Int = native.value
) extends DartObject:
  def inMilliseconds: Int = native.value
  def inSeconds: Int      = native.value
  def inMinutes: Int      = native.value

// `dart:async`'s `Timer`. `Timer.periodic(duration, callback)` is the
// canonical way to do periodic work in Flutter — commonly driven by
// `setState` inside the callback.
@native
@DartImport("dart:async")
class Timer extends DartObject:
  def cancel(): Unit = native.value
  def isActive: Boolean = native.value

@native
@DartImport("dart:async")
object Timer:
  def periodic(duration: Duration, callback: Timer => Unit): Timer = native.value
  def apply(duration: Duration, callback: () => Unit): Timer = native.value
