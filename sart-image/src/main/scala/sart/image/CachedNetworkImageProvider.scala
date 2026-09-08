package sart.image

import sart.dart.*
import flutter.material.ImageProvider

/** An `ImageProvider` backed by the cached-network-image pipeline — for the
 *  places a raw provider is wanted rather than the [[CachedNetworkImage]]
 *  widget (a `DecorationImage`, or `precacheImage` to warm the cache before a
 *  screen builds). Pass the shared [[CacheManager]] so it hits the same store
 *  as the widgets. */
@native
@DartImport("package:cached_network_image/cached_network_image.dart")
@DartPackage("cached_network_image", "^3.4.1")
class CachedNetworkImageProvider(
  url: String,
  cacheManager: CacheManager = native.value
) extends ImageProvider
