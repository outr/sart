package example.features

import flutter.material.*

// Feature fixture: Stack/Positioned/Align/Card/GestureDetector/Image
// facades, exercising z-axis layout, images, and gestures.

class LayoutFacadesExample extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    Card(
      elevation = 2.0,
      child = Stack(
        children = List(
          Image.network("https://flutter.dev/assets/logo.png"),
          Positioned(
            left = 16.0,
            bottom = 16.0,
            child = Align(
              alignment = Alignment.bottomLeft,
              child = GestureDetector(
                onTap = () => (),
                child = Text("tap me")
              )
            )
          )
        )
      )
    )
