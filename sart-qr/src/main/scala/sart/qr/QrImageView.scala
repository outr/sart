package sart.qr

import sart.dart.*
import flutter.material.{Widget, Key, Color, EdgeInsets}

/** A QR code as a Flutter widget (`qr_flutter`). `data` is the encoded string
 *  (required); the code auto-selects its version and uses error-correction
 *  level L by default. On TV this renders the device-pairing / deep-link code
 *  the phone scans — draw it on a white `backgroundColor` with a quiet-zone
 *  `padding` so a camera reads it reliably.
 */
@native
@DartImport("package:qr_flutter/qr_flutter.dart")
@DartPackage("qr_flutter", "^4.1.0")
class QrImageView(
  val data: String,
  val size: Double = native.value,
  val backgroundColor: Color = native.value,
  val padding: EdgeInsets = native.value,
  val semanticsLabel: String = native.value,
  val key: Key = native.value
) extends Widget
