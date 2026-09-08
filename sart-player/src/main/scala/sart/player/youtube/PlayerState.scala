package sart.player.youtube

import sart.dart.*

/** youtube_player_iframe player state. */
@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
@DartPackage("youtube_player_iframe", "^5.2.0")
class PlayerState extends DartObject

@native
@DartImport("package:youtube_player_iframe/youtube_player_iframe.dart")
object PlayerState:
  def unknown: PlayerState = native.value
  def unStarted: PlayerState = native.value
  def ended: PlayerState = native.value
  def playing: PlayerState = native.value
  def paused: PlayerState = native.value
  def buffering: PlayerState = native.value
  def cued: PlayerState = native.value
