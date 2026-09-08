package sart.tv

import flutter.services.LogicalKeyboardKey

/** The unified TV-remote button vocabulary, normalised across Apple TV,
 *  Samsung Tizen, LG webOS and Android TV. Flutter's platform embedders
 *  already fold most physical remote buttons onto `LogicalKeyboardKey`
 *  values; `TvKey` collapses those (and their per-platform synonyms) into
 *  one stable enum so app code matches on `TvKey.Select` instead of
 *  juggling `select` vs `enter` vs `gameButtonA` per device.
 *
 *  Transport keys (`PlayPause`, `FastForward`, …) are deliberately kept
 *  distinct from `Select`: on a remote, Play must always drive playback,
 *  never "click" the focused widget.
 */
enum TvKey:
  // Directional pad.
  case Up, Down, Left, Right
  // Activation and navigation.
  case Select, Back, Home, Menu
  // Media transport (kept separate from Select on purpose).
  case PlayPause, Play, Pause, Stop, FastForward, Rewind, Next, Previous
  // Broadcast / colour buttons (delivered on TV embedders that surface them).
  case ChannelUp, ChannelDown, Red, Green, Yellow, Blue
  // Numeric keys.
  case Digit0, Digit1, Digit2, Digit3, Digit4, Digit5, Digit6, Digit7, Digit8, Digit9

object TvKey:
  /** Decode a Flutter logical key into a [[TvKey]], or `None` if it isn't
   *  a recognised remote button. Keyed by `keyId` so it is independent of
   *  which embedder produced the event. */
  def fromLogicalKey(key: LogicalKeyboardKey): Option[TvKey] =
    byId.get(key.keyId)

  /** The reverse of [[fromLogicalKey]]'s intent, folding every synonym:
   *  the D-pad set, the "OK/select" set (select/enter/numpadEnter/
   *  gameButtonA/space) and the "back" set (goBack/escape/browserBack). */
  private lazy val byId: Map[Int, TvKey] = Map(
    LogicalKeyboardKey.arrowUp.keyId    -> Up,
    LogicalKeyboardKey.arrowDown.keyId  -> Down,
    LogicalKeyboardKey.arrowLeft.keyId  -> Left,
    LogicalKeyboardKey.arrowRight.keyId -> Right,
    // OK / select — fold every remote's activation button together.
    LogicalKeyboardKey.select.keyId      -> Select,
    LogicalKeyboardKey.enter.keyId       -> Select,
    LogicalKeyboardKey.numpadEnter.keyId -> Select,
    LogicalKeyboardKey.gameButtonA.keyId -> Select,
    LogicalKeyboardKey.space.keyId       -> Select,
    // Back — TV Back, desktop Escape, and the browser-back alias.
    LogicalKeyboardKey.goBack.keyId      -> Back,
    LogicalKeyboardKey.escape.keyId      -> Back,
    LogicalKeyboardKey.browserBack.keyId -> Back,
    LogicalKeyboardKey.contextMenu.keyId -> Menu,
    LogicalKeyboardKey.home.keyId        -> Home,
    // Media transport.
    LogicalKeyboardKey.mediaPlayPause.keyId      -> PlayPause,
    LogicalKeyboardKey.mediaPlay.keyId           -> Play,
    LogicalKeyboardKey.mediaPause.keyId          -> Pause,
    LogicalKeyboardKey.mediaStop.keyId           -> Stop,
    LogicalKeyboardKey.mediaFastForward.keyId    -> FastForward,
    LogicalKeyboardKey.mediaRewind.keyId         -> Rewind,
    LogicalKeyboardKey.mediaTrackNext.keyId      -> Next,
    LogicalKeyboardKey.mediaTrackPrevious.keyId  -> Previous,
    // Broadcast / colour buttons.
    LogicalKeyboardKey.channelUp.keyId    -> ChannelUp,
    LogicalKeyboardKey.channelDown.keyId  -> ChannelDown,
    LogicalKeyboardKey.colorF0Red.keyId    -> Red,
    LogicalKeyboardKey.colorF1Green.keyId  -> Green,
    LogicalKeyboardKey.colorF2Yellow.keyId -> Yellow,
    LogicalKeyboardKey.colorF3Blue.keyId   -> Blue,
    // Numeric keys.
    LogicalKeyboardKey.digit0.keyId -> Digit0,
    LogicalKeyboardKey.digit1.keyId -> Digit1,
    LogicalKeyboardKey.digit2.keyId -> Digit2,
    LogicalKeyboardKey.digit3.keyId -> Digit3,
    LogicalKeyboardKey.digit4.keyId -> Digit4,
    LogicalKeyboardKey.digit5.keyId -> Digit5,
    LogicalKeyboardKey.digit6.keyId -> Digit6,
    LogicalKeyboardKey.digit7.keyId -> Digit7,
    LogicalKeyboardKey.digit8.keyId -> Digit8,
    LogicalKeyboardKey.digit9.keyId -> Digit9
  )
