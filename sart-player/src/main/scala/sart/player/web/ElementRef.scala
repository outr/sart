package sart.player.web

import sart.dart.*

/** Mutable holder the (un-unregisterable) web view factory captures instead
 *  of the element directly, so [[WebVideo.dispose]] can null it and let the
 *  `<video>` element be garbage-collected. Emitted into `video_web.dart` so
 *  its package:web reference stays out of the shared bundle. */
@DartLibrary("platform/video_web.dart")
class ElementRef(var el: Option[HTMLVideoElement])
