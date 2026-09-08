package sart.image

import sart.dart.*
import sart.stdlib.Duration

/** Configuration for a [[CacheManager]] (`flutter_cache_manager`). `cacheKey`
 *  names the on-disk store; `stalePeriod` drops entries older than it and
 *  `maxNrOfCacheObjects` caps the count (LRU eviction past the cap) so disk
 *  usage stays bounded. */
@native
@DartImport("package:flutter_cache_manager/flutter_cache_manager.dart")
@DartPackage("flutter_cache_manager", "^3.4.1")
class Config(
  cacheKey: String,
  stalePeriod: Duration = native.value,
  maxNrOfCacheObjects: Int = native.value
) extends DartObject
