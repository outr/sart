package example.features

import flutter.material.*

// Feature fixture: styled containers using BoxDecoration, BoxShadow,
// BorderRadius, and Color factories.

class DecorationsExample extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    Container(
      width = 200.0,
      height = 120.0,
      decoration = BoxDecoration(
        color = Color.fromARGB(255, 200, 230, 255),
        borderRadius = BorderRadius.circular(12.0),
        boxShadow = List(
          BoxShadow(
            color = Colors.grey,
            offset = Offset(0.0, 4.0),
            blurRadius = 8.0
          )
        )
      ),
      child = Center(child = Text("Hello, styled world"))
    )
