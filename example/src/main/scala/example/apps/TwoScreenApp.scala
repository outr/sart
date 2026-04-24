package example.apps

import flutter.material.*

// Two-screen app: a home screen with a button that pushes a detail
// screen via Navigator. Exercises Navigator.of(context).push +
// MaterialPageRoute + a closure that returns a Widget.

class HomeScreen extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Home")),
      body = Center(
        child = ElevatedButton(
          onPressed = () =>
            Navigator.of(context).push(
              MaterialPageRoute[Unit](
                builder = (ctx: BuildContext) => DetailScreen()
              )
            ),
          child = Text("Go to detail")
        )
      )
    )

class DetailScreen extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Detail")),
      body = Center(child = Text("You made it!"))
    )
