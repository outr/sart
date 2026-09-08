package sart.tv

import sart.dart.*
import flutter.material.*
import flutter.services.{KeyEvent, KeyDownEvent}

/** A focusable, selectable surface — the leaf primitive of D-pad
 *  navigation. It joins Flutter's focus traversal (so the D-pad moves
 *  between `Focusable`s for free), rebuilds via [[builder]] whenever its
 *  focus changes so the app can render a focused vs unfocused look, and
 *  invokes [[onSelect]] when OK/Select is pressed while focused.
 *
 *  {{{
 *  Focusable(
 *    onSelect = () => open(movie),
 *    builder  = focused => Card(color = if focused then hi else base, child = poster)
 *  )
 *  }}}
 */
class Focusable(
  val onSelect: () => Unit,
  val builder: Boolean => Widget,
  val autofocus: Boolean = false
) extends StatefulWidget:
  override def createState(): State[Focusable] = FocusableState()

class FocusableState extends State[Focusable]:
  private var focused: Boolean = false

  override def build(context: BuildContext): Widget =
    Focus(
      autofocus = widget.autofocus,
      onFocusChange = f => setState(() => focused = f),
      onKeyEvent = (node, event) => handle(event),
      child = widget.builder(focused)
    )

  private def handle(event: DartObject): KeyEventResult =
    if !event.isInstanceOf[KeyDownEvent] then KeyEventResult.ignored
    else
      val decoded = TvKey.fromLogicalKey(cast[KeyEvent](event).logicalKey)
      if !decoded.isEmpty && decoded.get == TvKey.Select then
        widget.onSelect()
        KeyEventResult.handled
      else KeyEventResult.ignored
