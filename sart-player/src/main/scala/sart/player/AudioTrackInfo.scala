package sart.player

/** A selectable embedded audio track the player exposes. The picker only
 *  surfaces the list when there's more than one. Named `…Info` to avoid
 *  clashing with media_kit's own `AudioTrack` in the emitted Dart. */
case class AudioTrackInfo(
  id: String,
  label: String,
  language: Option[String] = Option.empty
)
