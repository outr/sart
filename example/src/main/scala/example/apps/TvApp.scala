package example.apps

import sart.dart.*
import sart.tv.*
import flutter.material.*

/** Demo of the reusable `sart-tv` library: a `RemoteControl` decoding
 *  remote key presses into [[TvKey]] values, a row of [[Focusable]] cards
 *  driven by the D-pad, a [[TvLifecycle]] handle, and the build-selected
 *  [[TvPlatform]]. Use ↑↓←→ to move focus, Enter/Select to pick a card.
 */
class TvApp extends StatefulWidget:
  override def createState(): State[TvApp] = TvAppState()

class TvAppState extends State[TvApp]:
  private var lastKey: String = "—"
  private var selected: Int = -1
  private var lifecycle: AppLifecycleListener = null

  override def initState(): Unit =
    super.initState()
    // On a real TV, pause/resume would release and reacquire the player.
    lifecycle = TvLifecycle(
      onPause = () => (),
      onResume = () => ()
    )

  override def dispose(): Unit =
    lifecycle.dispose()
    super.dispose()

  private def onKey(key: TvKey): Boolean =
    setState(() => lastKey = key.toString)
    // Let the D-pad fall through to Flutter's focus traversal; consume
    // everything else so it doesn't also trigger default handling.
    val directional =
      key == TvKey.Up || key == TvKey.Down || key == TvKey.Left || key == TvKey.Right
    !directional

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Sart TV")),
      body = RemoteControl(
        onKey = onKey,
        child = Center(
          child = Column(
            mainAxisAlignment = MainAxisAlignment.center,
            children = List(
              Text(s"Platform: ${TvPlatform.current}"),
              SizedBox(height = 8.0),
              Text(s"Last key: $lastKey"),
              SizedBox(height = 24.0),
              Row(
                mainAxisAlignment = MainAxisAlignment.center,
                children = List(card(0), gap, card(1), gap, card(2))
              ),
              SizedBox(height = 24.0),
              if selected >= 0 then Text(s"Selected card $selected")
              else Text("Nothing selected")
            )
          )
        )
      )
    )

  private def gap: Widget = SizedBox(width = 16.0)

  private def card(index: Int): Widget =
    Focusable(
      autofocus = index == 0,
      onSelect = () => setState(() => selected = index),
      builder = focused =>
        Container(
          width = 120.0,
          height = 120.0,
          decoration = BoxDecoration(
            color = if focused then Colors.blue else Colors.grey,
            borderRadius = BorderRadius.circular(12.0),
            border = Border.all(
              color = if focused then Colors.white else Colors.transparent,
              width = 3.0
            )
          ),
          child = Center(child = Text(s"Card $index"))
        )
    )
