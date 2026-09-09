package example.apps

import sart.dart.*
import sart.qr.QrImageView
import flutter.material.*

/** Demo of sart-qr's `qr_flutter` facade: a device-pairing QR on a rounded
 *  white card, the shape a TV app shows for the phone to scan. */
class QrApp extends StatelessWidget:
  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Sart QR Pairing")),
      body = Center(
        child = Column(
          mainAxisAlignment = MainAxisAlignment.center,
          children = List(
            Container(
              padding = EdgeInsets.all(16.0),
              decoration = BoxDecoration(
                color = Colors.white,
                borderRadius = BorderRadius.circular(18.0)
              ),
              // A camera reads a QR most reliably on white with a quiet zone,
              // so the card supplies the white and the code hugs it.
              child = QrImageView(
                data = "https://nabo.tv/link?code=DEMO42",
                size = 220.0,
                backgroundColor = Colors.white,
                padding = EdgeInsets.zero
              )
            ),
            SizedBox(height = 20.0),
            Text("Scan to pair this device")
          )
        )
      )
    )
