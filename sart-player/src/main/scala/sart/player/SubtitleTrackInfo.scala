package sart.player

/** A selectable subtitle track the player exposes (embedded or attached), as
 *  the picker sees it. Named `…Info` to avoid clashing with media_kit's own
 *  `SubtitleTrack` in the emitted Dart. */
case class SubtitleTrackInfo(
  id: String,
  label: String,
  language: Option[String] = Option.empty,
  codec: Option[String] = Option.empty
)
