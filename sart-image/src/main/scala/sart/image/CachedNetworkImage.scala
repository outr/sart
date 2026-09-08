package sart.image

import sart.dart.*
import sart.stdlib.Duration
import flutter.material.{Widget, BuildContext, Key, BoxFit, Alignment, Color}

/** A network image with disk + memory caching (`cached_network_image`), the
 *  workhorse for poster/backdrop artwork in a media catalogue. Give every
 *  widget the same shared [[CacheManager]] so artwork isn't re-downloaded, and
 *  `memCacheWidth` a decode-sized bound so a rail of thumbnails doesn't decode
 *  at full resolution.
 *
 *  `placeholder` renders while loading `(context, url)`; `errorWidget` renders
 *  on failure `(context, url, error)` — the third argument is the error, typed
 *  loosely since it's only ever shown, not inspected.
 */
@native
@DartImport("package:cached_network_image/cached_network_image.dart")
@DartPackage("cached_network_image", "^3.4.1")
class CachedNetworkImage(
  val imageUrl: String,
  val cacheManager: CacheManager = native.value,
  val fit: BoxFit = native.value,
  val width: Double = native.value,
  val height: Double = native.value,
  val alignment: Alignment = native.value,
  val color: Color = native.value,
  val fadeInDuration: Duration = native.value,
  val fadeOutDuration: Duration = native.value,
  val memCacheWidth: Int = native.value,
  val memCacheHeight: Int = native.value,
  val placeholder: (BuildContext, String) => Widget = native.value,
  val errorWidget: (BuildContext, String, Any) => Widget = native.value,
  val key: Key = native.value
) extends Widget
