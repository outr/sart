package example.apps

import sart.dart.*
import sart.player.*
import flutter.material.*
import scala.concurrent.ExecutionContext.Implicits.global

/** Demo of sart-player's YouTube backend (`YouTubeVideo` over
 *  youtube_player_iframe). `setSource` takes a YouTube videoId. Plugs into
 *  the same `VideoPlayer` interface as the media_kit / web backends.
 */
class YtApp extends StatefulWidget:
  override def createState(): State[YtApp] = YtAppState()

class YtAppState extends State[YtApp]:
  private var player: VideoPlayer = null
  private var ready: Boolean = false

  override def initState(): Unit =
    super.initState()
    load()

  private def load(): Unit =
    player = YouTubeVideo()
    await(player.setSource("aqz-KE-bpKQ")) // Big Buck Bunny (a YouTube videoId)
    setState(() => ready = true)

  override def dispose(): Unit =
    player.dispose()
    super.dispose()

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Sart YouTube")),
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
