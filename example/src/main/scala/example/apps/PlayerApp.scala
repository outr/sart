package example.apps

import sart.dart.*
import sart.player.*
import sart.tv.media.MediaItem
import flutter.material.*
import scala.concurrent.ExecutionContext.Implicits.global

/** Demo of the reusable `sart-player` library (a project dependency here;
 *  a published `sartLibraries` entry in a real app). Plays a video and an
 *  audio source through the general-purpose `PlayerController`.
 */
class PlayerApp extends StatefulWidget:
  override def createState(): State[PlayerApp] = PlayerAppState()

class PlayerAppState extends State[PlayerApp]:
  private var video: PlayerController = null
  private var audio: PlayerController = null
  private var ready: Boolean = false

  // initState must stay synchronous (Flutter asserts it returns void, not
  // a Future) — so the async setup runs in a separate fire-and-forget method.
  override def initState(): Unit =
    super.initState()
    load()

  private def load(): Unit =
    video = PlayerController(NetworkSource("sample.mp4"))
    audio = PlayerController(NetworkSource("sample.mp3"))
    await(video.initialize())
    await(audio.initialize())
    // Publish the video to the OS media session — lock-screen / TV
    // "now playing" and remote transport controls drive this player.
    await(
      video.bindMediaSession(
        MediaItem(id = "sample", title = "Sample video", artist = "sart-player")
      )
    )
    setState(() => ready = true)

  override def dispose(): Unit =
    video.dispose()
    audio.dispose()
    super.dispose()

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Sart Player")),
      body = Center(
        child = if !ready then CircularProgressIndicator()
        else Column(
          mainAxisAlignment = MainAxisAlignment.center,
          children = List(
            SizedBox(width = 320.0, child = video.view),
            SizedBox(height = 16.0),
            Row(
              mainAxisAlignment = MainAxisAlignment.center,
              children = List(
                ElevatedButton.icon(
                  onPressed = () => setState(() => video.play()),
                  icon = Icon(Icons.play_arrow),
                  label = Text("Play video")
                ),
                SizedBox(width = 12.0),
                ElevatedButton.icon(
                  onPressed = () => setState(() => video.pause()),
                  icon = Icon(Icons.pause),
                  label = Text("Pause")
                )
              )
            ),
            SizedBox(height = 12.0),
            ElevatedButton.icon(
              onPressed = () => setState(() => audio.play()),
              icon = Icon(Icons.music_note),
              label = Text("Play audio")
            )
          )
        )
      )
    )
