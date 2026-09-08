package sart.tv

import sart.dart.*
import flutter.material.*
import flutter.services.{KeyEvent, KeyDownEvent, KeyRepeatEvent}

/** Wraps [[child]] so TV-remote key presses arrive as [[TvKey]] values.
 *  Place one near the root of a screen: it takes focus, decodes each
 *  key-down (and key-repeat, for held buttons) event, and calls
 *  [[onKey]]. Return `true` to consume the key — stopping it from
 *  bubbling to Flutter's default focus traversal — or `false` to let it
 *  through, so directional navigation still works for keys you ignore.
 */
class RemoteControl(
  val onKey: TvKey => Boolean,
  val child: Widget,
  val autofocus: Boolean = true
) extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    Focus(
      autofocus = autofocus,
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
