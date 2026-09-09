package sart.lottie

import sart.dart.*
import flutter.material.{Widget, Key, BoxFit, BuildContext}

/** Lottie vector animations as Flutter widgets (`lottie`). The factories mirror
 *  Dart's static `Lottie.asset` / `Lottie.network` — each returns a widget that
 *  plays the animation. `name`/`url` is the first positional argument (pass it
 *  positionally). `repeat` loops it; `errorBuilder` `(context, error, stack)`
 *  renders a fallback when the animation can't load (e.g. a static icon).
 *
 *  Used on TV for the animated weather glyphs on the NaboClock (a Meteocons
 *  Lottie set), where an SVG or a baked frame sequence wouldn't scale smoothly.
 */
@native
@DartImport("package:lottie/lottie.dart")
@DartPackage("lottie", "^3.1.0")
object Lottie:
  /** Play a bundled animation from `assets/…` (declare it under `flutter:
   *  assets:` in the app's pubspec). */
  def asset(
    name: String,
    animate: Boolean = native.value,
    repeat: Boolean = native.value,
    reverse: Boolean = native.value,
    width: Double = native.value,
    height: Double = native.value,
    fit: BoxFit = native.value,
    errorBuilder: (BuildContext, Any, Any) => Widget = native.value,
    key: Key = native.value
  ): Widget = native.value

  /** Play an animation fetched from a URL. */
  def network(
    url: String,
    animate: Boolean = native.value,
    repeat: Boolean = native.value,
    reverse: Boolean = native.value,
    width: Double = native.value,
    height: Double = native.value,
    fit: BoxFit = native.value,
    errorBuilder: (BuildContext, Any, Any) => Widget = native.value,
    key: Key = native.value
  ): Widget = native.value
