package example.apps

import sart.dart.*
import sart.stdlib.Uri
import flutter.material.*
import example.player.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** Spike: a cross-platform video/audio player authored entirely in Scala,
 *  over the `video_player` facade. Proves Sart can build a real media
 *  player that actually plays — the de-risking slice for a general
 *  `sart-player` library extracted from NaboPlayer.
 */
class PlayerApp extends StatefulWidget:
  override def createState(): State[PlayerApp] = PlayerAppState()

class PlayerAppState extends State[PlayerApp]:
  private var video: VideoPlayerController = null
  private var audio: VideoPlayerController = null
  private var ready: Boolean = false

  override def initState(): Unit =
    super.initState()
    video = VideoPlayerController.networkUrl(Uri.parse("sample.mp4"))
    audio = VideoPlayerController.networkUrl(Uri.parse("sample.mp3"))
    await(video.initialize())
    await(audio.initialize())
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
            SizedBox(
              width = 320.0,
              height = 240.0,
              child = VideoPlayer(video)
            ),
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
