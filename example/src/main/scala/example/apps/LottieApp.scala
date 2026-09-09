package example.apps

import sart.dart.*
import sart.lottie.Lottie
import flutter.material.*

/** Demo of sart-lottie's `lottie` facade: a looping vector animation with a
 *  fallback icon if it can't load. Uses `Lottie.network` so the demo needs no
 *  bundled asset; NaboTV plays the equivalent from `assets/` on the NaboClock.
 */
class LottieApp extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Sart Lottie")),
      body = Center(
        child = Column(
          mainAxisAlignment = MainAxisAlignment.center,
          children = List(
            SizedBox(
              width = 240.0,
              height = 240.0,
              // A public sample animation; `errorBuilder` shows a static glyph
              // if it can't load, exactly as the weather icons fall back.
              child = Lottie.network(
                "https://lottie.host/2c8e8b3c-0b3f-4a3e-8e2a-9f7c0d6c1a5b/q2Y9m3Xk7d.json",
                fit = BoxFit.contain,
                repeat = true,
                errorBuilder = (_, _, _) => Icon(Icons.cloud, size = 96.0)
              )
            ),
            SizedBox(height = 20.0),
            Text("Looping vector animation")
          )
        )
      )
    )
