import '../main.dart';
import 'dart:async';
import 'dart:js_interop';
import 'dart:js_interop_unsafe';
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

/// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:19
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

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:29
  void fireEnded() {
    onEndedCb.foreach((cb) => cb());
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:30
  void fireError() {
    onErrorCb.foreach((cb) => cb());
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:31
  void bump() {
    if (!subCtrl.isClosed) {
      subCtrl.add(null);
    }
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:33
  @override
  void setOnEnded(void Function() cb) {
    onEndedCb = cb;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:34
  @override
  void setOnError(void Function() cb) {
    onErrorCb = cb;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:36
  @override
  bool get rendersImageSubtitles {
    return false;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:38
  @override
  Future<void> setSource(
    String url, {
    double startSeconds = 0.0,
    List<SubtitleSource> sideloaded = const [],
  }) async {
    if (startSeconds > 0.5) {
      element.addEventListener(
        'loadedmetadata',
        ((Event _$1) {
          element.currentTime = startSeconds;
        }).toJS,
      );
    }
    if (sideloaded.isNotEmpty) {
      (await addSubtitles(sideloaded));
    }
    final isHls = url.contains('.m3u8');
    final header = authHeaderVal;
    final nativeHls = element
        .canPlayType('application/vnd.apple.mpegurl')
        .isNotEmpty;
    if (isHls && ((header != null) || !nativeHls)) {
      if ((await WebVideo.ensureHls())) {
        hls.foreach((h) => h.destroy());
        final cfg = (header.map((h) => hlsConfigWithAuth(h)) ?? (JSObject()));
        final h = Hls(cfg);
        h.loadSource(url);
        h.attachMedia(element);
        hls = h;
        /* TODO expr Return (sart-player/src/main/scala/sart/player/web/WebVideo.scala:38) */
        ;
      } else {
        if ((header != null)) {
          console.error('[sart-player] hls.js failed to load'.toJS);
          fireError();
          /* TODO expr Return (sart-player/src/main/scala/sart/player/web/WebVideo.scala:38) */
          ;
        }
      }
    }
    element.src = url;
    return Future.value(null);
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:40
  double get setSource$default$2 {
    return 0.0;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:41
  List get setSource$default$3 {
    return [];
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:72
  @override
  Future<void> addSubtitles(List<SubtitleSource> subs) {
    subs.forEach(
      (s) => attachTrack(s.url, s.label, s.language, s.isDefault, false),
    );
    return Future.value(null);
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:76
  @override
  List<SubtitleTrackInfo> get subtitleTracks {
    final tt = element.textTracks;
    List<SubtitleTrackInfo> out = [];
    int i = 0;
    while (i < tt.length) {
      final t = tt[i];
      if ((t.kind == 'subtitles') || (t.kind == 'captions')) {
        out = [
          ...(out),
          ...([
            SubtitleTrackInfo(
              i.toString(),
              (t.label.isNotEmpty
                  ? t.label
                  : t.language.isNotEmpty
                  ? t.language.toUpperCase()
                  : 'Subtitles'),
              (t.language.isEmpty ? null : t.language),
              null,
            ),
          ]),
        ];
      }
      i = i + 1;
    }
    return out;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:94
  @override
  String? get currentSubtitleId {
    final tt = element.textTracks;
    String? found = null;
    int i = 0;
    while (i < tt.length) {
      if (tt[i].mode == 'showing') {
        found = i.toString();
      }
      i = i + 1;
    }
    return found;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:103
  @override
  Stream<void> get subtitleTracksStream {
    return subCtrl.stream;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:105
  @override
  void selectEmbeddedSubtitle(String id) {
    final idx = (int.tryParse(id) ?? (-1));
    final tt = element.textTracks;
    int i = 0;
    while (i < tt.length) {
      tt[i].mode = i == idx ? 'showing' : 'disabled';
      i = i + 1;
    }
    bump();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:114
  @override
  void selectUriSubtitle(String url, String? label, String? language) {
    attachTrack(url, label, language, false, true);
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:116
  Object? get selectUriSubtitle$default$2 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:117
  Object? get selectUriSubtitle$default$3 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:120
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

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:137
  @override
  void subtitlesOff() {
    final tt = element.textTracks;
    int i = 0;
    while (i < tt.length) {
      tt[i].mode = 'disabled';
      i = i + 1;
    }
    bump();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:146
  @override
  List<AudioTrackInfo> get audioTracks {
    return [];
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:147
  @override
  String? get currentAudioId {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:148
  @override
  Stream<void> get audioTracksStream {
    return subCtrl.stream;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:149
  @override
  void selectAudioTrack(String id) {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:151
  @override
  void seek(double seconds) {
    element.currentTime = seconds;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:153
  void applyVolume() {
    final v = (volumeLevel * gainLevel).clamp(0.0, 1.0);
    element.volume = v;
    element.muted = v <= 0;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:159
  @override
  void setVolume(double v) {
    volumeLevel = v;
    applyVolume();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:160
  @override
  void setGain(double g) {
    gainLevel = g;
    applyVolume();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:161
  @override
  void setRate(double v) {
    element.playbackRate = v;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:162
  @override
  void setLooping(bool v) {
    element.loop = v;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:163
  @override
  void setCover(bool v) {
    element.style.objectFit = v ? 'cover' : 'contain';
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:164
  @override
  void setAuthHeader(String? v) {
    authHeaderVal = v;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:165
  @override
  void setPreferredSubtitleId(String? id) {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:166
  @override
  void setAudioFocus(bool v) {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:167
  @override
  void setAudioOnly(bool v) {}

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:168
  @override
  Stream<List<double>>? get audioBands {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:172
  JSObject hlsConfigWithAuth(String header) {
    final cfg = JSObject();
    cfg.setProperty(
      'xhrSetup'.toJS,
      ((JSObject xhr, JSAny url) {
        final rs = xhr.getProperty('readyState'.toJS);
        if (((rs as JSNumber)).toDartInt == 0) {
          xhr.callMethod('open'.toJS, 'GET'.toJS, url);
        }
        return xhr.callMethod(
          'setRequestHeader'.toJS,
          'Authorization'.toJS,
          header.toJS,
        );
      }).toJS,
    );
    return cfg;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:185
  @override
  void play() {
    element.play();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:186
  @override
  void pause() {
    element.pause();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:188
  @override
  double get position {
    return element.currentTime;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:189
  @override
  bool get ready {
    return (duration > 0) || (position > 0);
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:191
  @override
  VideoSize? get videoSize {
    return (() {
      final w = element.videoWidth;
      final h = element.videoHeight;
      return (w > 0) && (h > 0) ? VideoSize(w, h) : null;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:196
  @override
  double get duration {
    return (() {
      final d = element.duration;
      return d.isFinite ? d : 0.0;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:200
  @override
  bool get paused {
    return element.paused;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:201
  @override
  bool get ended {
    return element.ended;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:203
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

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:211
  @override
  Widget view() {
    return HtmlElementView(viewType: viewType);
  }

  static int seq = 0;
  static bool scriptRequested = false;

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:217
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
      (_$2) => (ref.el ?? (HTMLVideoElement())),
    );
    final v = WebVideo(viewType, el, ref);
    el.addEventListener('ended', ((Event _$3) => v.fireEnded()).toJS);
    el.addEventListener('error', ((Event _$4) => v.fireError()).toJS);
    el.textTracks.addEventListener('addtrack', ((Event _$5) => v.bump()).toJS);
    el.textTracks.addEventListener(
      'removetrack',
      ((Event _$6) => v.bump()).toJS,
    );
    el.textTracks.addEventListener('change', ((Event _$7) => v.bump()).toJS);
    return v;
  }

  /// Source: sart-player/src/main/scala/sart/player/web/WebVideo.scala:242
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
