package flutter.ui

import sart.dart.*

// dart:ui types that collide with same-named widgets (`Image`) — aliased
// imports keep the emitted Dart unambiguous.

@native
@DartImport("dart:ui")
@DartAlias("ui")
class Image extends DartObject:
  def width: Int = native.value
  def height: Int = native.value
  def toByteData(format: ImageByteFormat = native.value): scala.concurrent.Future[sart.stdlib.Option[ByteData]] = native.value

@native
@DartImport("dart:ui")
@DartAlias("ui")
class ImageByteFormat extends DartObject

@native
@DartImport("dart:ui")
@DartAlias("ui")
object ImageByteFormat:
  val png: ImageByteFormat = native.value
  val rawRgba: ImageByteFormat = native.value

/** dart:typed_data's ByteData/ByteBuffer — enough surface to turn a
 *  captured image into a `List<int>` for base64/file-save.
 */
@native
@DartImport("dart:typed_data")
class ByteData extends DartObject:
  def buffer: ByteBuffer = native.value
  def lengthInBytes: Int = native.value

@native
@DartImport("dart:typed_data")
class ByteBuffer extends DartObject:
  def asUint8List(): sart.stdlib.Uint8List = native.value
