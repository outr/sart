package example.apps

import flutter.material.*
import sart.stdlib.{Duration, Timer, Option, Some, None}

// A simple stopwatch — exercises Timer.periodic driving setState for
// visible per-second UI updates. Uses `Option[Timer]` so Dart's sound
// null safety is happy and the `.map` call translates through the
// Option Dart shim.

class ClockApp extends StatefulWidget:
  override def createState(): State[ClockApp] = ClockAppState()

class ClockAppState extends State[ClockApp]:
  private var seconds: Int = 0
  private var timer: Option[Timer] = None

  private def start(): Unit =
    setState { () =>
      timer = Some(
        Timer.periodic(
          Duration(seconds = 1),
          t => setState(() => seconds = seconds + 1)
        )
      )
    }

  private def stop(): Unit =
    setState { () =>
      timer.foreach(t => t.cancel())
      timer = None
    }

  private def reset(): Unit =
    setState(() => seconds = 0)

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Stopwatch")),
      body = Center(
        child = Column(
          mainAxisAlignment = MainAxisAlignment.center,
          children = List(
            Text("Elapsed: " + seconds.toString + "s"),
            Row(
              mainAxisAlignment = MainAxisAlignment.center,
              children = List(
                ElevatedButton(onPressed = () => start(), child = Text("Start")),
                ElevatedButton(onPressed = () => stop(),  child = Text("Stop")),
                ElevatedButton(onPressed = () => reset(), child = Text("Reset"))
              )
            )
          )
        )
      )
    )
