package example

import flutter.runApp
import flutter.material.*

@main def sartMain(): Unit =
  runApp(MyApp())

class MyApp extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    MaterialApp(
      title = "Flutter Demo",
      theme = ThemeData(
        colorScheme = ColorScheme.fromSeed(seedColor = Colors.deepPurple),
        useMaterial3 = true
      ),
      home = MyHomePage(title = "Flutter Demo Home Page")
    )

class MyHomePage(val title: String) extends StatefulWidget:
  override def createState(): State[MyHomePage] =
    MyHomePageState()

class MyHomePageState extends State[MyHomePage]:
  private var counter: Int = 0

  private def incrementCounter(): Unit =
    setState(() => counter = counter + 1)

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(
        backgroundColor = Theme.of(context).colorScheme.inversePrimary,
        title = Text(widget.title)
      ),
      body = Center(
        child = Column(
          mainAxisAlignment = MainAxisAlignment.center,
          children = List(
            Text("You have pushed the button this many times:"),
            Text(
              s"$counter",
              style = Theme.of(context).textTheme.headlineMedium
            )
          )
        )
      ),
      floatingActionButton = FloatingActionButton(
        onPressed = () => incrementCounter(),
        tooltip = "Increment",
        child = Icon(Icons.add)
      )
    )
