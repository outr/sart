package sart.tv

import sart.dart.*
import flutter.material.*
import flutter.services.{KeyEvent, KeyDownEvent, KeyRepeatEvent}

/** Wraps [[child]] so TV-remote key presses arrive as [[TvKey]] values.
 *  Place one near the root of a screen: as key events bubble up from
 *  whatever [[Focusable]] currently has focus, it decodes each key-down
 *  (and key-repeat, for held buttons) event and calls [[onKey]]. Return
 *  `true` to consume the key, or `false` to let it through — so
 *  directional navigation still moves focus for keys you ignore.
 *
 *  It never takes focus itself (`canRequestFocus = false`): the focus
 *  targets are the [[Focusable]]s inside `child`, so give one of them
 *  `autofocus = true` for the D-pad to have somewhere to start.
 */
class RemoteControl(
  val onKey: TvKey => Boolean,
  val child: Widget
) extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    Focus(
      canRequestFocus = false,
      onKeyEvent = (node, event) => handle(event),
      child = child
    )

  private def handle(event: DartObject): KeyEventResult =
    val isPress =
      event.isInstanceOf[KeyDownEvent] || event.isInstanceOf[KeyRepeatEvent]
    if !isPress then KeyEventResult.ignored
    else
      val decoded = TvKey.fromLogicalKey(cast[KeyEvent](event).logicalKey)
      if decoded.isEmpty then KeyEventResult.ignored
      else if onKey(decoded.get) then KeyEventResult.handled
      else KeyEventResult.ignored
