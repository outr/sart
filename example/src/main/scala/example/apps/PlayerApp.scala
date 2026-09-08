package example.apps

import sart.dart.*
import sart.player.*
import flutter.material.*
import scala.concurrent.ExecutionContext.Implicits.global

/** Demo of the reusable `sart-player` library — the faithful port of
 *  NaboPlayer's `NaboVideo` over media_kit (libmpv). Loads a video through
 *  the platform backend selected by `VideoPlayer.create()`.
 */
class PlayerApp extends StatefulWidget:
  override def createState(): State[PlayerApp] = PlayerAppState()

class PlayerAppState extends State[PlayerApp]:
  private var player: VideoPlayer = null
  private var ready: Boolean = false

  // initState stays synchronous; the async load runs fire-and-forget.
  override def initState(): Unit =
    super.initState()
    load()

  private def load(): Unit =
    player = VideoPlayer.create()
    await(player.setSource("sample.mp4"))
    setState(() => ready = true)

  override def dispose(): Unit =
    player.dispose()
    super.dispose()

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Sart Player")),
      body = Center(
        child =
          if !ready then CircularProgressIndicator()
          else
            Column(
              mainAxisAlignment = MainAxisAlignment.center,
              children = List(
                SizedBox(width = 480.0, height = 270.0, child = player.view()),
                SizedBox(height = 16.0),
                Row(
                  mainAxisAlignment = MainAxisAlignment.center,
                  children = List(
                    ElevatedButton.icon(
                      onPressed = () => player.play(),
                      icon = Icon(Icons.play_arrow),
                      label = Text("Play")
                    ),
                    SizedBox(width = 12.0),
                    ElevatedButton.icon(
                      onPressed = () => player.pause(),
                      icon = Icon(Icons.pause),
                      label = Text("Pause")
                    )
                  )
                )
              )
            )
      )
    )
