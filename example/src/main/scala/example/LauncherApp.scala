package example

import flutter.runApp
import flutter.material.*
import example.apps.*

// `@main` entry for the Sart example app. Launches a hub that routes
// into each bundled demo. Each row's `build` callback is invoked with
// a fresh `BuildContext` when the user taps through.

@main def sartMain(): Unit = runApp(LauncherApp())

case class Demo(
  title: String,
  subtitle: String,
  build: BuildContext => Widget
)

class LauncherApp extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    MaterialApp(
      title = "Sart Showcase",
      theme = ThemeData(
        colorScheme = ColorScheme.fromSeed(seedColor = Colors.deepPurple),
        useMaterial3 = true
      ),
      home = LauncherHome()
    )

class LauncherHome extends StatelessWidget:
  private val demos: List[Demo] = List(
    Demo("Showcase",    "Kitchen-sink feature demo",    ctx => ShowcaseApp()),
    Demo("Counter",     "Classic Flutter counter",      ctx => MyHomePage(title = "Counter")),
    Demo("Todos",       "TextField + list + state",     ctx => TodoApp()),
    Demo("Dice",        "Random + history + Navigator", ctx => DiceApp()),
    Demo("Stopwatch",   "Timer.periodic",               ctx => ClockApp()),
    Demo("Two-screen",  "Navigator.push demo",          ctx => HomeScreen())
  )

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(
        title = Text("Sart Demos"),
        backgroundColor = Theme.of(context).colorScheme.inversePrimary
      ),
      body = ListView.builder(
        itemCount = demos.size,
        itemBuilder = (ctx: BuildContext, i: Int) =>
          val d: Demo = demos(i)
          ListTile(
            leading = Icon(Icons.menu),
            title = Text(d.title),
            subtitle = Text(d.subtitle),
            onTap = () => Navigator.of(ctx).push(
              MaterialPageRoute[Unit](builder = d.build)
            )
          )
      )
    )
