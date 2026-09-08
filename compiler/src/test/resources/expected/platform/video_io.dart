import '../main.dart';
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';
import '../sart_option.dart';

/// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:21
class MediaKitVideo extends VideoPlayer {
  MediaKitVideo() {
    controller = VideoController(player);
    player.stream.completed.listen((done) {
      if (done && !endedFired) {
        endedFired = true;
        onEndedCb.foreach((cb) => cb());
      }
    });
    player.stream.error.listen((_$1) => onErrorCb.foreach((cb) => cb()));
  }

  final Player player = Player(
    configuration: PlayerConfiguration(logLevel: MPVLogLevel.info),
  );
  late VideoController controller;
  void Function()? onEndedCb = null;
  void Function()? onErrorCb = null;
  bool endedFired = false;
  bool disposed = false;
  bool coverFit = false;
  double volumeLevel = 1.0;
  double gainLevel = 1.0;

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:44
  void cmd(Future<void> Function() op) {
    if (!disposed) {
      op();
    }
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:47
  @override
  void setOnEnded(void Function() cb) {
    onEndedCb = cb;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:48
  @override
  void setOnError(void Function() cb) {
    onErrorCb = cb;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:50
  @override
  VideoSize? get videoSize {
    return (() {
      final w = (player.state.width ?? (0));
      final h = (player.state.height ?? (0));
      return (w > 0) && (h > 0) ? VideoSize(w, h) : null;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:55
  @override
  bool get rendersImageSubtitles {
    return false;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:57
  @override
  Future<void> setSource(
    String url, {
    double startSeconds = 0.0,
    List<SubtitleSource> sideloaded = const [],
  }) async {
    return disposed
        ? Future.value(null)
        : (await (() async {
            endedFired = false;
            try {
              if (startSeconds > 0.5) {
                (await player.open(Media(url), play: false));
                (await seekWhenReady(startSeconds));
                (await player.play());
              } else {
                (await player.open(Media(url)));
              }
              if (sideloaded.isNotEmpty) {
                (await addSubtitles(sideloaded));
              }
            } on Object {
              if (!disposed) {
                null;
              }
            }
            return Future.value(null);
          })());
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:59
  double get setSource$default$2 {
    return 0.0;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:60
  List get setSource$default$3 {
    return [];
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:77
  Future<void> seekWhenReady(double seconds) async {
    if (player.state.duration.inMilliseconds <= 0) {
      (await player.stream.duration.firstWhere((d) => d.inMilliseconds > 0));
    }
    (await player.seek(Duration(milliseconds: (seconds * 1000).round())));
    return Future.value(null);
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:83
  @override
  Future<void> addSubtitles(List<SubtitleSource> subs) async {
    return (await (() async {
      final defaults = subs.where((s) => s.isDefault).toList();
      return defaults.isEmpty
          ? Future.value(null)
          : (await (() async {
              final pick = defaults.first;
              try {
                (await player.setSubtitleTrack(
                  SubtitleTrack.uri(
                    pick.url,
                    title: pick.label,
                    language: pick.language,
                  ),
                ));
              } on Object {
                null;
              }
              return Future.value(null);
            })());
    })());
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:96
  @override
  List<SubtitleTrackInfo> get subtitleTracks {
    return player.state.tracks.subtitle
        .where((t) => (t.id != 'no') && (t.id != 'auto'))
        .toList()
        .map(
          (t) => SubtitleTrackInfo(
            t.id,
            (t.title ??
                ((t.language.map((l) => l.toUpperCase()) ?? ('Subtitles')))),
            t.language,
            t.codec,
          ),
        )
        .toList();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:108
  @override
  String? get currentSubtitleId {
    return (() {
      final s = player.state.track.subtitle;
      return (s.id == 'no') || (s.id == 'auto') ? null : s.id;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:112
  @override
  Stream<void> get subtitleTracksStream {
    return player.stream.tracks.map((_$2) => null);
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:114
  @override
  void selectEmbeddedSubtitle(String id) {
    cmd(
      () => player.setSubtitleTrack(
        ((() {
              final r = player.state.tracks.subtitle
                  .where((s) => s.id == id)
                  .toList();
              return r.isEmpty ? null : r.first;
            })() ??
            (SubtitleTrack.no())),
      ),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:118
  @override
  void selectUriSubtitle(String url, String? label, String? language) {
    cmd(
      () => player.setSubtitleTrack(
        SubtitleTrack.uri(url, title: label, language: language),
      ),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:120
  Object? get selectUriSubtitle$default$2 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:121
  Object? get selectUriSubtitle$default$3 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:125
  @override
  void subtitlesOff() {
    cmd(() => player.setSubtitleTrack(SubtitleTrack.no()));
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:127
  @override
  List<AudioTrackInfo> get audioTracks {
    return player.state.tracks.audio
        .where((t) => (t.id != 'no') && (t.id != 'auto'))
        .toList()
        .map(
          (t) => AudioTrackInfo(
            t.id,
            (t.title ??
                ((t.language.map((l) => l.toUpperCase()) ?? ('Audio')))),
            t.language,
          ),
        )
        .toList();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:138
  @override
  String? get currentAudioId {
    return (() {
      final a = player.state.track.audio;
      return (a.id == 'no') || (a.id == 'auto') ? null : a.id;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:142
  @override
  Stream<void> get audioTracksStream {
    return player.stream.tracks.map((_$3) => null);
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:144
  @override
  void selectAudioTrack(String id) {
    cmd(
      () => player.setAudioTrack(
        ((() {
              final r = player.state.tracks.audio
                  .where((a) => a.id == id)
                  .toList();
              return r.isEmpty ? null : r.first;
            })() ??
            (AudioTrack.auto())),
      ),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:148
  @override
  void seek(double seconds) {
    cmd(() => player.seek(Duration(milliseconds: (seconds * 1000).round())));
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:151
  void applyVolume() {
    cmd(
      () =>
          player.setVolume(((volumeLevel * gainLevel) * 100).clamp(0.0, 200.0)),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:154
  @override
  void setVolume(double v) {
    volumeLevel = v;
    applyVolume();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:155
  @override
  void setGain(double g) {
    gainLevel = g;
    applyVolume();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:156
  @override
  void setLooping(bool v) {
    cmd(
      () => player.setPlaylistMode(v ? PlaylistMode.loop : PlaylistMode.none),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:158
  @override
  void setCover(bool v) {
    coverFit = v;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:159
  @override
  void setPreferredSubtitleId(String? id) {}

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:160
  @override
  void setAuthHeader(String? v) {}

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:161
  @override
  void setAudioFocus(bool v) {}

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:162
  @override
  void setAudioOnly(bool v) {}

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:163
  @override
  Stream<List<double>>? get audioBands {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:165
  @override
  void play() {
    cmd(() => player.play());
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:166
  @override
  void pause() {
    cmd(() => player.pause());
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:167
  @override
  void setRate(double v) {
    cmd(() => player.setRate(v));
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:169
  @override
  double get position {
    return player.state.position.inMilliseconds / 1000.0;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:170
  @override
  double get duration {
    return player.state.duration.inMilliseconds / 1000.0;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:171
  @override
  bool get paused {
    return !player.state.playing;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:172
  @override
  bool get ended {
    return (() {
      final dms = player.state.duration.inMilliseconds;
      return (dms > 0) && (player.state.position.inMilliseconds >= dms);
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:176
  @override
  void dispose() {
    if (!disposed) {
      disposed = true;
      (() {
        player.dispose();
      })();
    }
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:181
  @override
  Widget view() {
    return Video(
      controller: controller,
      controls: NoVideoControls,
      fit: coverFit ? BoxFit.cover : BoxFit.contain,
    );
  }
}

/// Source: sart-player/src/main/scala/sart/player/VideoBackendFactoryIo.scala:10
class VideoBackendFactory {
  VideoBackendFactory._();

  /// Source: sart-player/src/main/scala/sart/player/VideoBackendFactoryIo.scala:11
  static VideoPlayer create() {
    MediaKit.ensureInitialized();
    return MediaKitVideo();
  }
}
