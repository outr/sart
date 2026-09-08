import '../main.dart';
import 'dart:async';
import 'dart:js_interop';
import 'dart:ui_web';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:web/web.dart';
import '../sart_option.dart';

/// Source: sart-player/src/main/scala/sart/player/VideoBackendFactoryWeb.scala:10
class VideoBackendFactory {
  VideoBackendFactory._();

  /// Source: sart-player/src/main/scala/sart/player/VideoBackendFactoryWeb.scala:11
  static VideoPlayer create() {
    return WebVideo.create();
  }
}

/// Source: sart-player/src/main/scala/sart/player/web/ElementRef.scala:10
class ElementRef {
  HTMLVideoElement? el;
  ElementRef(this.el);
}

/// Source: sart-player/src/main/scala/sart/player/web/Hls.scala:18
@JS('window.Hls')
external JSAny? get hlsGlobal;

/// Source: sart-player/src/main/scala/sart/player/web/Hls.scala:10
@JS('Hls')
extension type Hls._(JSObject _) implements JSObject {
  external Hls(JSObject config);

  /// Source: sart-player/src/main/scala/sart/player/web/Hls.scala:11
  external void loadSource(String url);

  /// Source: sart-player/src/main/scala/sart/player/web/Hls.scala:12
  external void attachMedia(HTMLVideoElement video);

  /// Source: sart-player/src/main/scala/sart/player/web/Hls.scala:13
  external void destroy();
}

/// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:22
class WebVideo extends VideoPlayer {
  final String viewType;
  final HTMLVideoElement element;
  final ElementRef ref;
  WebVideo(this.viewType, this.element, this.ref);

  Hls? hls = null;
  String? authHeaderVal = null;
  void Function()? onEndedCb = null;
  void Function()? onErrorCb = null;
  final StreamController<void> subCtrl = StreamController.broadcast();
  double volumeLevel = 1.0;
  double gainLevel = 1.0;

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:32
  void bump() {
    if (!subCtrl.isClosed) {
      subCtrl.add(null);
    }
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:36
  @override
  void setOnEnded(void Function() cb) {
    onEndedCb = cb;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:37
  @override
  void setOnError(void Function() cb) {
    onErrorCb = cb;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:39
  @override
  bool get rendersImageSubtitles {
    return false;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:41
  @override
  Future<void> setSource(
    String url, {
    double startSeconds = 0.0,
    List<SubtitleSource> sideloaded = const [],
  }) async {
    if (sideloaded.isNotEmpty) {
      (await addSubtitles(sideloaded));
    }
    final isHls = url.contains('.m3u8');
    final header = authHeaderVal;
    final nativeHls = element
        .canPlayType('application/vnd.apple.mpegurl')
        .isNotEmpty;
    if (isHls && !nativeHls) {
      if ((await WebVideo.ensureHls())) {
        hls.foreach((h) => h.destroy());
        final h = Hls(JSObject());
        h.loadSource(url);
        h.attachMedia(element);
        hls = h;
        /* TODO expr Return (sart-player/src/main/scala/sart/player/web/WebVideo.scala:41) */
        ;
      } else {
        if ((header != null)) {
          console.error('[sart-player] hls.js failed to load'.toJS);
        }
      }
    }
    element.src = url;
    return Future.value(null);
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:43
  double get setSource$default$2 {
    return 0.0;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:44
  List get setSource$default$3 {
    return [];
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:67
  @override
  Future<void> addSubtitles(List<SubtitleSource> subs) {
    subs.forEach(
      (s) => attachTrack(s.url, s.label, s.language, s.isDefault, false),
    );
    return Future.value(null);
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:74
  @override
  List<SubtitleTrackInfo> get subtitleTracks {
    return [];
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:75
  @override
  String? get currentSubtitleId {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:76
  @override
  Stream<void> get subtitleTracksStream {
    return subCtrl.stream;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:77
  @override
  void selectEmbeddedSubtitle(String id) {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:78
  @override
  void subtitlesOff() {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:80
  @override
  void selectUriSubtitle(String url, String? label, String? language) {
    attachTrack(url, label, language, false, true);
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:82
  Object? get selectUriSubtitle$default$2 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:83
  Object? get selectUriSubtitle$default$3 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:86
  void attachTrack(
    String url,
    String? label,
    String? language,
    bool isDefault,
    bool show,
  ) {
    final track = HTMLTrackElement();
    track.kind = 'subtitles';
    track.src = url;
    track.srclang = (language ?? ('en'));
    track.label = (label ?? ((language ?? ('Subtitles'))));
    if (isDefault) {
      track.setAttribute('default', '');
    }
    element.appendChild(track);
    if (show) {
      track.track.mode = 'showing';
    }
    bump();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:104
  @override
  List<AudioTrackInfo> get audioTracks {
    return [];
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:105
  @override
  String? get currentAudioId {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:106
  @override
  Stream<void> get audioTracksStream {
    return subCtrl.stream;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:107
  @override
  void selectAudioTrack(String id) {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:109
  @override
  void seek(double seconds) {
    element.currentTime = seconds;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:111
  void applyVolume() {
    final v = (volumeLevel * gainLevel).clamp(0.0, 1.0);
    element.volume = v;
    element.muted = v <= 0;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:117
  @override
  void setVolume(double v) {
    volumeLevel = v;
    applyVolume();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:118
  @override
  void setGain(double g) {
    gainLevel = g;
    applyVolume();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:119
  @override
  void setRate(double v) {
    element.playbackRate = v;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:120
  @override
  void setLooping(bool v) {
    element.loop = v;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:121
  @override
  void setCover(bool v) {
    element.style.objectFit = v ? 'cover' : 'contain';
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:122
  @override
  void setAuthHeader(String? v) {
    authHeaderVal = v;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:123
  @override
  void setPreferredSubtitleId(String? id) {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:124
  @override
  void setAudioFocus(bool v) {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:125
  @override
  void setAudioOnly(bool v) {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:126
  @override
  Stream<List<double>>? get audioBands {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:128
  @override
  void play() {
    element.play();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:129
  @override
  void pause() {
    element.pause();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:131
  @override
  double get position {
    return element.currentTime;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:132
  @override
  bool get ready {
    return (duration > 0) || (position > 0);
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:134
  @override
  VideoSize? get videoSize {
    return (() {
      final w = element.videoWidth;
      final h = element.videoHeight;
      return (w > 0) && (h > 0) ? VideoSize(w, h) : null;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:139
  @override
  double get duration {
    return (() {
      final d = element.duration;
      return d.isFinite ? d : 0.0;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:143
  @override
  bool get paused {
    return element.paused;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:144
  @override
  bool get ended {
    return element.ended;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:146
  @override
  void dispose() {
    element.pause();
    hls.foreach((h) => h.destroy());
    element.removeAttribute('src');
    element.load();
    if (!subCtrl.isClosed) {
      subCtrl.close();
    }
    ref.el = null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:154
  @override
  Widget view() {
    return HtmlElementView(viewType: viewType);
  }

  static int seq = 0;
  static bool scriptRequested = false;

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:160
  static WebVideo create() {
    seq = seq + 1;
    final viewType = 'sart-video-${seq}';
    final el = HTMLVideoElement();
    el.controls = false;
    el.autoplay = true;
    el.style.width = '100%';
    el.style.height = '100%';
    el.style.backgroundColor = 'black';
    el.style.pointerEvents = 'none';
    final ref = ElementRef(el);
    platformViewRegistry.registerViewFactory(
      viewType,
      (_$1) => (ref.el ?? (HTMLVideoElement())),
    );
    return WebVideo(viewType, el, ref);
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:179
  static Future<bool> ensureHls() async {
    return (hlsGlobal != null)
        ? Future.value(true)
        : (await (() async {
            if (!scriptRequested) {
              scriptRequested = true;
              final script = HTMLScriptElement();
              script.src = '/hls.min.js';
              script.async = true;
              document.head.foreach((h) => h.appendChild(script));
            }
            int i = 0;
            while ((i < 100) && (hlsGlobal == null)) {
              (await Future.delayed(Duration(milliseconds: 100), () => null));
              i = i + 1;
            }
            return Future.value((hlsGlobal != null));
          })());
  }
}
