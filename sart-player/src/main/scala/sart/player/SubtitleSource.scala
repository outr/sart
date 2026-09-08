package sart.player


/** A subtitle track to attach to the player from a URL (sideloaded, i.e.
 *  not embedded in the media). `isDefault` marks the one to enable on load. */
case class SubtitleSource(
  url: String,
  language: Option[String] = Option.empty,
  label: Option[String] = Option.empty,
  isDefault: Boolean = false,
  id: Option[String] = Option.empty
)
