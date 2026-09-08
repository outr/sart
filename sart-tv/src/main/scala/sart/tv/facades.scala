package sart.tv

import sart.dart.*

/** Flutter's `AppLifecycleListener` (package:flutter/widgets.dart) — the
 *  modern, callback-based replacement for a `WidgetsBindingObserver`
 *  subclass. Construct it (registers with the binding), keep the handle,
 *  and call [[dispose]] when done. [[TvLifecycle]] wraps this with
 *  Scala-friendly defaults; this facade is exposed for direct use too.
 */
@native
@DartImport("package:flutter/widgets.dart")
class AppLifecycleListener(
  val onResume: () => Unit = native.value,
  val onPause: () => Unit = native.value,
  val onInactive: () => Unit = native.value,
  val onHide: () => Unit = native.value,
  val onShow: () => Unit = native.value,
  val onRestart: () => Unit = native.value,
  val onDetach: () => Unit = native.value
) extends DartObject:
  def dispose(): Unit = native.value
