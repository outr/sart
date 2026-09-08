package sart.image

import sart.dart.*
import sart.stdlib.Duration

/** A `flutter_cache_manager` cache manager — the disk-backed store behind
 *  [[CachedNetworkImage]] and [[CachedNetworkImageProvider]]. Build one from a
 *  [[Config]] (bounded LRU: cap the object count and stale period so browsing
 *  a large catalogue never grows the cache without limit), share the instance
 *  across every image widget, and pass it as their `cacheManager`.
 */
@native
@DartImport("package:flutter_cache_manager/flutter_cache_manager.dart")
@DartPackage("flutter_cache_manager", "^3.4.1")
class CacheManager(config: Config) extends DartObject
