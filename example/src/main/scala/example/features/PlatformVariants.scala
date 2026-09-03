package example.features

import sart.dart.*

/** Platform-variant emission (docs/design/platform-variants.md): one
 *  `@native` facade object is the switch — call sites import
 *  `platform/platform_name.dart`, which Sart GENERATES as a conditional
 *  export over the `@DartLibrary` implementations below. Each
 *  implementation emits into its own library file with its own import
 *  header, so `dart:io` / `package:web` facades used by one variant
 *  never reach the others.
 */
@native
@DartImport("platform/platform_name.dart")
@DartVariants(
  default = "platform/platform_name_stub.dart",
  io      = "platform/platform_name_io.dart",
  web     = "platform/platform_name_web.dart"
)
object PlatformName:
  def describe(): String = native.value

@DartLibrary("platform/platform_name_stub.dart")
@DartName("PlatformName")
object PlatformNameStub:
  def describe(): String = "unknown platform"

@DartLibrary("platform/platform_name_io.dart")
@DartName("PlatformName")
object PlatformNameIo:
  def describe(): String = "native (dart.library.io)"

@DartLibrary("platform/platform_name_web.dart")
@DartName("PlatformName")
object PlatformNameWeb:
  def describe(): String = "web (dart.library.js_interop)"

/** App code sees only the facade. */
class PlatformVariantDemo:
  def platformLabel(): String = PlatformName.describe()
