package example.apps

import sart.dart.*
import sart.stdlib.Duration
import sart.image.{CachedNetworkImage, CacheManager, Config}
import flutter.material.*

/** Demo of sart-image's `cached_network_image` facade: a wall of poster-shaped
 *  network images sharing one bounded disk [[CacheManager]], each with a
 *  loading placeholder and an error fallback. Mirrors how a TV catalogue draws
 *  its artwork.
 */
class ImageApp extends StatelessWidget:
  // One shared, bounded cache for every tile — artwork isn't re-downloaded and
  // disk usage stays capped no matter how far the catalogue is browsed.
  private val cache: CacheManager = CacheManager(
    // `cacheKey` is a positional Dart param — pass it positionally so the
    // facade doesn't emit it as a (rejected) named argument.
    Config(
      "sartDemoImages",
      stalePeriod = Duration(days = 7),
      maxNrOfCacheObjects = 200
    )
  )

  // Stable public placeholder images (picsum) standing in for TMDB posters.
  private val urls: List[String] =
    List(237, 238, 239, 240, 241, 242, 243, 244).map(id =>
      s"https://picsum.photos/id/$id/300/450"
    )

  private def poster(url: String): Widget =
    ClipRRect(
      borderRadius = BorderRadius.circular(8.0),
      child = CachedNetworkImage(
        imageUrl = url,
        cacheManager = cache,
        fit = BoxFit.cover,
        width = 120.0,
        height = 180.0,
        // Decode at the display size, not the source size — a rail of these
        // shouldn't hold full-resolution bitmaps in memory.
        memCacheWidth = 240,
        fadeInDuration = Duration(milliseconds = 200),
        placeholder = (_, _) =>
          Container(
            width = 120.0,
            height = 180.0,
            color = Colors.black12,
            child = Center(child = CircularProgressIndicator())
          ),
        errorWidget = (_, _, _) =>
          Container(
            width = 120.0,
            height = 180.0,
            color = Colors.black12,
            child = Icon(Icons.broken_image)
          )
      )
    )

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Sart Cached Images")),
      body = SingleChildScrollView(
        padding = EdgeInsets.all(16.0),
        child = Wrap(
          spacing = 12.0,
          runSpacing = 12.0,
          children = urls.map(poster)
        )
      )
    )
