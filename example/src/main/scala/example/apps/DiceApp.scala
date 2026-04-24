package example.apps

import flutter.material.*
import sart.stdlib.Random
// Scala's built-in Option/Some/None already map to Dart `T?` at emit time,
// so we don't need sart.stdlib's variants here — and using Scala's lets
// `List.lastOption` just work.

// Benchmark port: a dice roller with per-face history and a detail screen.
// Deliberately exercises the 1.0 surface under realistic use:
//
//   • StatefulWidget + setState with a mutable history list
//   • Random from sart.stdlib
//   • case class for history entries (including copyWith / pattern match)
//   • Navigator.push to a detail route
//   • ListView.builder rendering a dynamic list
//   • Card / BoxDecoration for polished cards
//   • IconButton + Icon for the clear action
//   • Option[Roll] for the most-recent-roll slot
//
// Kept as a parallel app alongside the counter; counter remains the
// `@main` entry so `sartRun` still launches it.

case class Roll(face: Int, index: Int)

class DiceApp extends StatefulWidget:
  override def createState(): State[DiceApp] = DiceAppState()

class DiceAppState extends State[DiceApp]:
  private val rng: Random = Random()
  private var history: List[Roll] = List()

  private def roll(): Unit =
    setState { () =>
      val next = Roll(rng.nextInt(6) + 1, history.size + 1)
      history = history :+ next
    }

  private def clearHistory(): Unit =
    setState(() => history = List())

  private def latestRoll: Option[Roll] =
    history.lastOption

  private def faceLabel(o: Option[Roll]): String =
    o.fold("—")(r => "⚀⚁⚂⚃⚄⚅".substring(r.face - 1, r.face))

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(
        title = Text("Dice"),
        backgroundColor = Theme.of(context).colorScheme.inversePrimary
      ),
      body = Column(
        mainAxisAlignment = MainAxisAlignment.center,
        children = List(
          // Current face
          Center(
            child = Container(
              width = 120.0,
              height = 120.0,
              decoration = BoxDecoration(
                color = Colors.white,
                borderRadius = BorderRadius.circular(16.0),
                boxShadow = List(
                  BoxShadow(color = Colors.grey, blurRadius = 8.0)
                )
              ),
              child = Center(child = Text(faceLabel(latestRoll)))
            )
          ),
          Row(
            mainAxisAlignment = MainAxisAlignment.center,
            children = List(
              ElevatedButton(onPressed = () => roll(),          child = Text("Roll")),
              ElevatedButton(onPressed = () => clearHistory(),   child = Text("Clear"))
            )
          ),
          Text("Rolled " + history.size.toString + " times")
        )
      ),
      floatingActionButton = FloatingActionButton(
        onPressed = () => Navigator.of(context).push(
          MaterialPageRoute[Unit](
            builder = (ctx: BuildContext) => HistoryScreen(history = history)
          )
        ),
        tooltip = "History",
        child = Icon(Icons.menu)
      )
    )

class HistoryScreen(val history: List[Roll]) extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("History")),
      body = ListView.builder(
        itemCount = history.size,
        itemBuilder = (ctx: BuildContext, i: Int) =>
          ListTile(
            leading = Icon(Icons.check),
            title = Text("Roll #" + history(i).index.toString),
            trailing = Text(history(i).face.toString)
          )
      )
    )
