package sart.tv

import sart.stdlib.DartString

/** Which TV platform the app is running on.
 *
 *  Apple TV, Samsung Tizen, LG webOS and Android TV all present as
 *  "native" (`dart.library.io`) to Flutter's conditional imports, so the
 *  export-switch mechanism can't tell them apart at compile time, and
 *  runtime OS probes differ per embedder fork (and one may throw on
 *  another's host). `TvPlatform` is therefore selected DETERMINISTICALLY
 *  from a `--dart-define=SART_TV_PLATFORM=<name>` value, which the sbt-sart
 *  TV build tasks inject automatically: `sartTvOS` → `appletv`,
 *  `sartTizen` → `tizen`, `sartWebOS` → `webos`. Unset ⇒ [[Other]].
 */
enum TvPlatform:
  case AppleTV, Tizen, WebOS, AndroidTV, Other

object TvPlatform:
  /** The compile-time `--dart-define=SART_TV_PLATFORM` value (or `""`). */
  private val configured: String =
    DartString.fromEnvironment("SART_TV_PLATFORM")

  /** The platform selected by the build. */
  def current: TvPlatform = fromName(configured)

  /** True on any of the four TV platforms — i.e. a define was supplied. */
  def isTv: Boolean = current != Other

  /** Map a `SART_TV_PLATFORM` string (case-sensitive, lower-case) to a
   *  platform, accepting both the short name and a vendor synonym. */
  def fromName(name: String): TvPlatform =
    if name == "appletv" || name == "tvos" then AppleTV
    else if name == "tizen" || name == "samsung" then Tizen
    else if name == "webos" || name == "lg" then WebOS
    else if name == "androidtv" || name == "android-tv" then AndroidTV
    else Other
